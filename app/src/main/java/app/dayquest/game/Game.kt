package app.dayquest.game

import app.dayquest.data.CheckItem
import app.dayquest.data.GameDao
import app.dayquest.data.Hero
import app.dayquest.data.LogEntry
import app.dayquest.data.LogKind
import app.dayquest.data.Pet
import app.dayquest.data.SettingsStore
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/** What one action did, for the floating numbers, particles and the undo bar. */
data class Outcome(
    val xp: Double = 0.0,
    val gold: Double = 0.0,
    val hp: Double = 0.0,
    val boss: Double = 0.0,
    val crit: Boolean = false,
    val drop: String = "",
    val levelUp: Int = 0,
    val fainted: Boolean = false,
    val bossDefeated: Boss? = null,
    val logId: Long = 0,
)

/** The day that just ended: what was missed, and what it cost. */
data class Offers(val main: List<Boss>, val seasonal: Boss?, val weekly: Boss?, val season: Season)

data class DayReport(
    val day: Long,
    val missed: Int,
    val hp: Double,
    val bossHit: Double,
    val fainted: Boolean,
    val resting: Boolean,
)

class Game(private val db: GameDao, private val settings: SettingsStore, private val random: Random = Random.Default) {
    private val mutex = Mutex()
    private val _fx = MutableSharedFlow<Outcome>(extraBufferCapacity = 16)
    val fx: SharedFlow<Outcome> = _fx

    suspend fun today(): Long = Rules.logicalDay(LocalDateTime.now(), settings.now().dayStart)

    suspend fun season(): Season = Season.of(LocalDate.ofEpochDay(today()), settings.now().southern)

    fun stats(h: Hero): Stats {
        val base = Stats(h.level / 2, h.level / 2, h.level / 2, h.level / 2)
        return listOf(h.weapon, h.armor, h.head, h.shield).mapNotNull { Catalog.gear(it)?.stats }.fold(base, Stats::plus)
    }

    suspend fun hero(): Hero = db.hero() ?: Hero(lastDay = today()).also { db.saveHero(it) }

    suspend fun createHero(name: String, skin: Int, hairStyle: Int, hairColor: Int) = mutex.withLock {
        val h = db.hero() ?: Hero(lastDay = today())
        db.saveHero(h.copy(name = name, skin = skin, hairStyle = hairStyle, hairColor = hairColor, lastDay = today()))
    }

    suspend fun updateLook(name: String, skin: Int, hairStyle: Int, hairColor: Int) = mutex.withLock {
        db.saveHero(hero().copy(name = name, skin = skin, hairStyle = hairStyle, hairColor = hairColor))
    }

    // ---- Scoring -------------------------------------------------------------------------

    suspend fun scoreHabit(taskId: Long, up: Boolean): Outcome? = mutex.withLock {
        val t = db.task(taskId) ?: return null
        val h = hero()
        val day = today()
        val delta = Rules.delta(t.value, up)
        val out = if (up) {
            val (h2, o) = gain(h, t.difficulty, delta, 1.0, t.id, LogKind.UP, day)
            db.saveHero(h2)
            db.update(t.copy(value = t.value + delta, countUp = t.countUp + 1))
            o
        } else {
            val (h2, o) = hurt(h, t.difficulty, delta, t.id, LogKind.DOWN, day)
            db.saveHero(h2)
            db.update(t.copy(value = t.value + delta, countDown = t.countDown + 1))
            o
        }
        emit(out)
    }

    /** Ticks or unticks a daily for [day] (today, or yesterday from the morning recap). */
    suspend fun setDaily(taskId: Long, done: Boolean, day: Long? = null): Outcome? = mutex.withLock {
        val t = db.task(taskId) ?: return null
        val d = day ?: today()
        if (done && t.doneDay != d) {
            val delta = Rules.delta(t.value, true)
            val bonus = Rules.streakBonus(t.streak) * checklistBonus(db.checklist(t.id))
            val (h2, o) = gain(hero(), t.difficulty, delta, bonus, t.id, LogKind.DAILY, d)
            db.saveHero(h2)
            val streak = t.streak + 1
            db.update(t.copy(doneDay = d, streak = streak, bestStreak = maxOf(t.bestStreak, streak), value = t.value + delta))
            emit(o)
        } else if (!done && t.doneDay == d) {
            val entry = db.lastLog(t.id, LogKind.DAILY)
            if (entry != null) revert(entry)
            val fresh = db.task(t.id) ?: t
            db.update(fresh.copy(doneDay = -1, streak = (fresh.streak - 1).coerceAtLeast(0)))
            null
        } else null
    }

    suspend fun setTodo(taskId: Long, done: Boolean): Outcome? = mutex.withLock {
        val t = db.task(taskId) ?: return null
        if (done && !t.done) {
            val delta = Rules.delta(t.value, true)
            val (h2, o) = gain(hero(), t.difficulty, delta, checklistBonus(db.checklist(t.id)), t.id, LogKind.TODO, today())
            db.saveHero(h2)
            db.update(t.copy(done = true, doneAt = System.currentTimeMillis(), value = t.value + delta))
            emit(o)
        } else if (!done && t.done) {
            db.lastLog(t.id, LogKind.TODO)?.let { revert(it) }
            val fresh = db.task(t.id) ?: t
            db.update(fresh.copy(done = false, doneAt = 0))
            null
        } else null
    }

    /** Spends gold on a custom reward. Returns null when the hero can't afford it. */
    suspend fun buyReward(taskId: Long): Outcome? = mutex.withLock {
        val t = db.task(taskId) ?: return null
        val h = hero()
        if (h.gold + 1e-9 < t.cost) return null
        db.saveHero(h.copy(gold = h.gold - t.cost))
        val id = db.insertLog(LogEntry(taskId = t.id, day = today(), kind = LogKind.REWARD, gold = -t.cost))
        Outcome(gold = -t.cost, logId = id)
    }

    suspend fun buyHealthPotion(): Outcome? = mutex.withLock {
        val h = hero()
        if (h.gold + 1e-9 < Rules.HEALTH_POTION_COST || h.hp >= Rules.MAX_HP) return null
        val heal = minOf(Rules.HEALTH_POTION_HP, Rules.MAX_HP - h.hp)
        db.saveHero(h.copy(gold = h.gold - Rules.HEALTH_POTION_COST, hp = h.hp + heal))
        val id = db.insertLog(LogEntry(day = today(), kind = LogKind.POTION, gold = -Rules.HEALTH_POTION_COST, hp = heal))
        emit(Outcome(gold = -Rules.HEALTH_POTION_COST, hp = heal, logId = id))
    }

    /** Takes back one action exactly as it was logged. */
    suspend fun undo(logId: Long) = mutex.withLock {
        val e = db.logEntry(logId) ?: return
        if (e.undone) return
        revert(e)
        val t = db.task(e.taskId) ?: return
        when (e.kind) {
            LogKind.UP -> db.update(t.copy(countUp = (t.countUp - 1).coerceAtLeast(0)))
            LogKind.DOWN -> db.update(t.copy(countDown = (t.countDown - 1).coerceAtLeast(0)))
            LogKind.DAILY -> db.update(t.copy(doneDay = -1, streak = (t.streak - 1).coerceAtLeast(0)))
            LogKind.TODO -> db.update(t.copy(done = false, doneAt = 0))
            else -> Unit
        }
    }

    private suspend fun revert(e: LogEntry) {
        val h = hero()
        var questHp = h.questHp
        if (h.quest.isNotEmpty() && e.boss > 0) {
            questHp = minOf(questHp + e.boss, Catalog.boss(h.quest)?.hp ?: questHp)
        }
        db.saveHero(
            h.copy(
                xp = (h.xp - e.xp).coerceAtLeast(0.0),
                gold = (h.gold - e.gold).coerceAtLeast(0.0),
                hp = (h.hp - e.hp).coerceIn(1.0, Rules.MAX_HP),
                questHp = questHp,
                dropsToday = if (e.drop.isNotEmpty()) (h.dropsToday - 1).coerceAtLeast(0) else h.dropsToday,
            )
        )
        if (e.drop.isNotEmpty()) db.addItem(e.drop, -1)
        if (e.valueDelta != 0.0) db.task(e.taskId)?.let { db.update(it.copy(value = it.value - e.valueDelta)) }
        db.updateLog(e.copy(undone = true))
    }

    private fun checklistBonus(items: List<CheckItem>): Double =
        if (items.isEmpty()) 1.0 else 1.0 + 0.5 * items.count { it.done } / items.size

    private suspend fun gain(h: Hero, d: Difficulty, delta: Double, bonus: Double, taskId: Long, kind: LogKind, day: Long): Pair<Hero, Outcome> {
        val s = stats(h)
        val xp = Rules.xpGain(delta, d, s, bonus)
        val gold = Rules.goldGain(delta, d, s, bonus)
        val boss = Catalog.boss(h.quest)
        val crit = boss != null && random.nextDouble() < Rules.critChance(s)
        val hit = if (boss != null) Rules.bossDamage(delta, d, s, crit) else 0.0
        val drop = rollDrop(h, d, s, season())
        if (drop.isNotEmpty()) db.addItem(drop, 1)

        var next = h.copy(xp = h.xp + xp, gold = h.gold + gold, questHp = h.questHp - hit, dropsToday = h.dropsToday + if (drop.isNotEmpty()) 1 else 0)
        var defeated: Boss? = null
        if (boss != null && next.questHp <= 0) {
            defeated = boss
            boss.rewards.forEach { (key, n) -> db.addItem(key, n) }
            next = next.copy(quest = "", questHp = 0.0, questsDone = next.withWin(boss.record), xp = next.xp + boss.xp, gold = next.gold + boss.gold)
            db.insertLog(LogEntry(day = day, kind = LogKind.VICTORY, gold = boss.gold, xp = boss.xp))
        }
        val before = next.level
        next = levelUp(next)
        val id = db.insertLog(LogEntry(taskId = taskId, day = day, kind = kind, xp = xp, gold = gold, valueDelta = delta, boss = hit, drop = drop))
        return next to Outcome(xp = xp, gold = gold, boss = hit, crit = crit, drop = drop, levelUp = if (next.level > before) next.level else 0, bossDefeated = defeated, logId = id)
    }

    private suspend fun hurt(h: Hero, d: Difficulty, delta: Double, taskId: Long, kind: LogKind, day: Long): Pair<Hero, Outcome> {
        val dmg = Rules.damage(delta, d, stats(h))
        val id = db.insertLog(LogEntry(taskId = taskId, day = day, kind = kind, hp = -dmg, valueDelta = delta))
        val hurt = h.copy(hp = h.hp - dmg)
        return if (hurt.hp <= 0) faint(hurt) to Outcome(hp = -dmg, fainted = true, logId = id)
        else hurt to Outcome(hp = -dmg, logId = id)
    }

    private fun levelUp(h: Hero): Hero {
        var x = h
        while (x.xp >= Rules.xpToNext(x.level)) {
            x = x.copy(xp = x.xp - Rules.xpToNext(x.level), level = x.level + 1, hp = Rules.MAX_HP)
        }
        return x
    }

    /** Fainting costs a level, the purse and the boss fight's progress; gear and pets are kept. */
    private fun faint(h: Hero) = h.copy(
        level = (h.level - 1).coerceAtLeast(1), xp = 0.0, gold = 0.0, hp = Rules.MAX_HP, deaths = h.deaths + 1,
        questHp = Catalog.boss(h.quest)?.hp ?: 0.0,
    )

    private fun rollDrop(h: Hero, d: Difficulty, s: Stats, season: Season): String {
        if (h.dropsToday >= Rules.dropCap(h.level)) return ""
        if (random.nextDouble() >= Rules.dropChance(d, s)) return ""
        val rares = Catalog.bosses.filter { h.defeated(it.key) }.flatMap { b -> b.rewards.map { it.first } }
            .filter { k -> k.startsWith("egg:") && Species.of(k.substringAfter(':'))?.questOnly == true || k.startsWith("potion:") && Potion.of(k.substringAfter(':'))?.questOnly == true }
        if (rares.isNotEmpty() && random.nextDouble() < 0.08) return rares[random.nextInt(rares.size)]
        val roll = random.nextInt(100)
        return when {
            roll < 35 -> "egg:" + Catalog.basicEggs().let { it[random.nextInt(it.size)] }.key
            // A fifth of potions found in a season are that season's own: the only way besides its boss.
            roll < 70 && random.nextInt(5) == 0 -> "potion:" + Potion.of(season).key
            roll < 70 -> "potion:" + Catalog.basicPotions().let { it[random.nextInt(it.size)] }.key
            else -> "food:" + Food.entries[random.nextInt(Food.entries.size)].key
        }
    }

    private fun emit(o: Outcome): Outcome {
        _fx.tryEmit(o)
        return o
    }

    // ---- New day -------------------------------------------------------------------------

    /** Dailies that were due on the last played day and not ticked: what the morning recap asks about. */
    suspend fun pendingRecap(): Pair<Long, List<Task>>? {
        val h = db.hero() ?: return null
        val today = today()
        if (today <= h.lastDay) return null
        return h.lastDay to missedOn(h.lastDay)
    }

    private suspend fun missedOn(day: Long): List<Task> {
        val dayStart = settings.now().dayStart
        return db.tasks().filter {
            it.type == TaskType.DAILY && Rules.isDue(it, day) && it.doneDay != day &&
                Rules.logicalDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(it.createdAt), ZoneId.systemDefault()), dayStart) <= day
        }
    }

    /** Closes the last played day: missed dailies hurt (unless resting), streaks break, a new day begins. */
    suspend fun startNewDay(): DayReport? = mutex.withLock {
        val h = db.hero() ?: return null
        val today = today()
        if (today <= h.lastDay) return null
        val day = h.lastDay
        val missed = missedOn(day)
        val s = stats(h)
        var hp = 0.0
        var bossHit = 0.0
        val boss = Catalog.boss(h.quest)
        val tasks = db.tasks()
        val updated = tasks.mapNotNull { t ->
            when (t.type) {
                TaskType.DAILY -> if (t in missed && !h.resting) {
                    val delta = Rules.delta(t.value, false)
                    val dmg = Rules.damage(delta, t.difficulty, s)
                    hp += dmg
                    if (boss != null) bossHit += boss.str * t.difficulty.mult.coerceAtLeast(0.5) * 1.5 * Rules.defense(s)
                    db.insertLog(LogEntry(taskId = t.id, day = day, kind = LogKind.MISSED, hp = -dmg, valueDelta = delta))
                    t.copy(value = t.value + delta, streak = 0)
                } else null
                TaskType.TODO -> if (!t.done && !h.resting) t.copy(value = t.value + Rules.delta(t.value, false) * 0.5) else null
                TaskType.HABIT -> t.copy(
                    countUp = 0, countDown = 0,
                    value = if (t.countUp == 0 && t.countDown == 0) t.value * 0.9 else t.value,
                )
                TaskType.REWARD -> null
            }
        }
        db.updateTasks(updated)
        db.resetDailyChecklists()
        if (bossHit > 0) db.insertLog(LogEntry(day = day, kind = LogKind.BOSS, hp = -bossHit))
        var next = h.copy(hp = h.hp - hp - bossHit, lastDay = today, dropsToday = 0)
        val fainted = next.hp <= 0
        if (fainted) next = faint(next)
        db.saveHero(next)
        if (hp + bossHit > 0) _fx.tryEmit(Outcome(hp = -(hp + bossHit), fainted = fainted))
        DayReport(day, missed.size, hp, bossHit, fainted, h.resting)
    }

    suspend fun setResting(rest: Boolean) = mutex.withLock { db.saveHero(hero().copy(resting = rest)) }

    // ---- Shop, stable, quests ------------------------------------------------------------

    suspend fun buyGear(g: Gear): Boolean = mutex.withLock {
        val h = hero()
        if (h.owns(g.key) || h.level < g.level || h.gold + 1e-9 < g.price) return false
        if (g.season != null && g.season != season()) return false
        db.saveHero(equipped(h.copy(gold = h.gold - g.price, ownedGear = h.ownedGear + "," + g.key), g))
        db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -g.price))
        true
    }

    suspend fun equip(g: Gear) = mutex.withLock {
        val h = hero()
        if (h.owns(g.key)) db.saveHero(equipped(h, g))
    }

    suspend fun unequip(slot: Slot) = mutex.withLock {
        val h = hero()
        db.saveHero(
            when (slot) {
                Slot.HEAD -> h.copy(head = "")
                Slot.SHIELD -> h.copy(shield = "")
                Slot.WEAPON, Slot.ARMOR -> h
            }
        )
    }

    private fun equipped(h: Hero, g: Gear) = when (g.slot) {
        Slot.WEAPON -> h.copy(weapon = g.key)
        Slot.ARMOR -> h.copy(armor = g.key)
        Slot.HEAD -> h.copy(head = g.key)
        Slot.SHIELD -> h.copy(shield = g.key)
    }

    suspend fun buyScene(sc: SceneDef): Boolean = mutex.withLock {
        val h = hero()
        if (h.ownsScene(sc.key)) {
            db.saveHero(h.copy(scene = sc.key)); return true
        }
        if (h.gold + 1e-9 < sc.price) return false
        db.saveHero(h.copy(gold = h.gold - sc.price, ownedScenes = h.ownedScenes + "," + sc.key, scene = sc.key))
        db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -sc.price))
        true
    }

    suspend fun hatch(s: Species, p: Potion): Boolean = mutex.withLock {
        val key = Catalog.petKey(s, p)
        if (db.pet(key) != null) return false
        val egg = "egg:${s.key}"
        val potion = "potion:${p.key}"
        if ((db.item(egg)?.count ?: 0) < 1 || (db.item(potion)?.count ?: 0) < 1) return false
        db.addItem(egg, -1)
        db.addItem(potion, -1)
        db.savePet(Pet(key))
        val h = hero()
        if (h.pet.isEmpty()) db.saveHero(h.copy(pet = key))
        true
    }

    /** Returns how much the pet grew, 0 when nothing happened. */
    suspend fun feed(petKey: String, food: Food): Int = mutex.withLock {
        val pet = db.pet(petKey) ?: return 0
        if (pet.growth >= 100 || (db.item("food:${food.key}")?.count ?: 0) < 1) return 0
        val species = Catalog.petParts(petKey)?.first ?: return 0
        val grow = if (species.favorite == food) Rules.FAVORITE_FOOD else Rules.OTHER_FOOD
        db.addItem("food:${food.key}", -1)
        db.savePet(pet.copy(growth = (pet.growth + grow).coerceAtMost(100)))
        grow
    }

    suspend fun setCompanion(petKey: String) = mutex.withLock {
        db.saveHero(hero().copy(pet = petKey))
    }

    /** The fights on offer right now: the next rank of each main boss, this season's boss and this week's. */
    suspend fun offers(h: Hero): Offers {
        val date = LocalDate.ofEpochDay(today())
        val southern = settings.now().southern
        val s = Season.of(date, southern)
        val (wy, wk) = Catalog.weekOf(date)
        return Offers(
            main = Catalog.bosses.map { Catalog.nextFight(it, h.wins(it.key)) },
            seasonal = Catalog.seasonalBoss(s, Season.year(date, southern), h.level).takeUnless { h.defeated(it.record) },
            weekly = Catalog.weekly(wy, wk, h.level).takeUnless { h.defeated(it.record) },
            season = s,
        )
    }

    suspend fun startQuest(b: Boss): Boolean = mutex.withLock {
        val h = hero()
        if (h.level < b.level || h.quest.isNotEmpty()) return false
        db.saveHero(h.copy(quest = b.key, questHp = b.hp))
        true
    }

    // ---- Home ----------------------------------------------------------------------------

    suspend fun buyDecor(d: Decor): Boolean = mutex.withLock {
        val h = hero()
        if (h.ownsDecor(d.key)) return true
        if (h.level < d.level || h.gold + 1e-9 < d.price) return false
        db.saveHero(h.copy(gold = h.gold - d.price, ownedDecor = h.ownedDecor + "," + d.key))
        db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -d.price))
        true
    }

    /** Puts an owned item in a spot (or clears it with null), or applies a wallpaper / floor. */
    suspend fun place(spot: Spot?, d: Decor?) = mutex.withLock {
        val h = hero()
        if (d != null && !h.ownsDecor(d.key)) return
        when {
            d?.type == DecorType.WALLPAPER -> db.saveHero(h.copy(wallpaper = d.key))
            d?.type == DecorType.FLOOR -> db.saveHero(h.copy(floor = d.key))
            spot != null -> {
                // One item lives in one spot: moving it clears the old one.
                val m = HomeCatalog.placement(h.home).filterValues { it != d?.key }.toMutableMap()
                if (d == null) m.remove(spot) else m[spot] = d.key
                db.saveHero(h.copy(home = HomeCatalog.encode(m)))
            }
        }
    }

    // ---- Achievements --------------------------------------------------------------------

    suspend fun facts(h: Hero): Facts {
        val pets = db.pets()
        val wins = h.wins
        val created = Rules.logicalDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(h.createdAt), ZoneId.systemDefault()), settings.now().dayStart)
        return Facts(
            streak = db.bestStreak(),
            together = (today() - created).toInt(),
            activeDays = db.activeDays(),
            tasks = db.doneCount(),
            level = h.level,
            bosses = wins.values.sum(),
            weekly = wins.filterKeys { it.startsWith("week:") }.size,
            seasons = wins.filterKeys { it.startsWith("season:") }.size,
            // Wins at mythic rank: the 4th and later win against the same main boss.
            mythic = Catalog.bosses.sumOf { ((wins[it.key] ?: 0) - Catalog.MYTHIC).coerceAtLeast(0) },
            pets = pets.size,
            grown = pets.count { it.growth >= 100 },
            decor = h.ownedDecor.split(',').count { HomeCatalog.item(it)?.let { d -> d.price > 0 } == true },
        )
    }

    /** Awards anything newly reached (with its gold) and returns it, for the celebration. */
    suspend fun checkAchievements(): List<Achievement> = mutex.withLock {
        val h = db.hero() ?: return emptyList()
        if (h.name.isEmpty()) return emptyList()
        val have = h.achievements.split(',').toSet()
        val fresh = Achievements.earned(facts(h)).filter { it.key !in have }
        if (fresh.isEmpty()) return emptyList()
        val gold = fresh.sumOf { it.gold }
        db.saveHero(h.copy(achievements = (have.filter { it.isNotEmpty() } + fresh.map { it.key }).joinToString(","), gold = h.gold + gold))
        db.insertLog(LogEntry(day = today(), kind = LogKind.ACHIEVEMENT, gold = gold))
        fresh
    }

    suspend fun abandonQuest() = mutex.withLock {
        db.saveHero(hero().copy(quest = "", questHp = 0.0))
    }
}
