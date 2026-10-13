package app.dayquest.game

import app.dayquest.data.CheckItem
import app.dayquest.data.GameDao
import app.dayquest.data.Hero
import app.dayquest.data.LogEntry
import app.dayquest.data.LogKind
import app.dayquest.data.Pet
import app.dayquest.data.Settings
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
    /** Every daily due today is done: fireworks and a chest. */
    val perfect: Boolean = false,
    /** Gold rolled the morning card's 5x jackpot. */
    val jackpot: Boolean = false,
    /** The morning card's bounty just paid out. */
    val bounty: Boolean = false,
    val bountyGold: Double = 0.0,
    /** Items from a bounty or a card's gift. */
    val items: List<String> = emptyList(),
    /** Mana gained (or spent). */
    val mp: Double = 0.0,
    /** The skill that was cast, for its own sound and effect. */
    val skill: Skill? = null,
    /** Weekend dungeon: the room just cleared (1-based, 0 for none), and whether it was the last. */
    val room: Int = 0,
    val dungeonDone: Boolean = false,
    val roomGold: Double = 0.0,
    val roomItems: List<String> = emptyList(),
)

/** What a chest held. */
data class Loot(val gold: Double, val xp: Double, val items: List<String>, val levelUp: Int)

/** The day that just ended: what was missed, and what it cost. */
data class Offers(val main: List<Boss>, val seasonal: List<Boss>, val weekly: Boss?, val season: Season)

data class DayReport(
    val day: Long,
    val missed: Int,
    val hp: Double,
    val bossHit: Double,
    val fainted: Boolean,
    val resting: Boolean,
    /** What the companion brought home overnight, or "". */
    val found: String = "",
)

/** All the rules that touch the database. [prefs] reads the current settings (a function, so tests need no Android). */
class Game(private val db: GameDao, private val prefs: suspend () -> Settings, private val random: Random = Random.Default) {
    private val mutex = Mutex()
    private val _fx = MutableSharedFlow<Outcome>(extraBufferCapacity = 16)
    val fx: SharedFlow<Outcome> = _fx

    suspend fun today(): Long = Rules.logicalDay(LocalDateTime.now(), prefs().dayStart)

    suspend fun season(): Season = Season.of(LocalDate.ofEpochDay(today()), prefs().southern)

    private suspend fun challenge(): Challenge = prefs().challenge

    /** Today's health potion price: the merchant's card halves it. */
    suspend fun potionCost(): Double = Rules.HEALTH_POTION_COST * DayEvent.of(today()).potion

    /** What the hero's companion and mount add right now. */
    private suspend fun boost(h: Hero): Boost =
        (if (h.pet.isEmpty()) Boost() else Companions.boost(db.pet(h.pet))) +
            (if (h.mount.isEmpty()) Boost() else Companions.mountBoost(db.pet(h.mount)))

    fun stats(h: Hero): Stats {
        val base = Stats(h.level / 2, h.level / 2, h.level / 2, h.level / 2) + Classes.bonus(Classes.of(h), h.level)
        return listOf(h.weapon, h.armor, h.head, h.shield).mapNotNull { Catalog.gear(it)?.stats }.fold(base, Stats::plus)
    }

    /** Extra XP and gold from friends in the tavern with fresh hero cards. */
    private suspend fun party(day: Long): Double = Party.bonus(db.friends().mapNotNull { Party.decode(it.code) }, day)

    suspend fun partyBonus(): Double = party(today())

    suspend fun hero(): Hero = db.hero() ?: Hero(lastDay = today(), uid = newUid()).also { db.saveHero(it) }

    private fun newUid() = java.util.UUID.randomUUID().toString().replace("-", "").take(16)

    suspend fun createHero(name: String, skin: Int, hairStyle: Int, hairColor: Int) = mutex.withLock {
        val h = db.hero() ?: Hero(lastDay = today())
        db.saveHero(h.copy(name = name, skin = skin, hairStyle = hairStyle, hairColor = hairColor, lastDay = today(), uid = h.uid.ifEmpty { newUid() }))
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
            val streakBonus = 1.0 + (Rules.streakBonus(t.streak) - 1.0) * DayEvent.of(d).streak
            val bonus = streakBonus * checklistBonus(db.checklist(t.id))
            val (h2, o) = gain(hero(), t.difficulty, delta, bonus, t.id, LogKind.DAILY, d)
            db.saveHero(h2)
            val streak = t.streak + 1
            db.update(t.copy(doneDay = d, streak = streak, bestStreak = maxOf(t.bestStreak, streak), value = t.value + delta))
            // Undoing the last daily keeps the chest, but a day pays only one.
            val h3 = hero()
            if (d == today() && h3.perfectDay != d && isPerfect(d)) {
                db.saveHero(h3.copy(perfectDay = d, chests = h3.chests + 1))
                emit(o.copy(perfect = true))
            } else emit(o)
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
        val cost = potionCost()
        if (h.gold + 1e-9 < cost || h.hp >= Rules.MAX_HP) return null
        val heal = minOf(Rules.HEALTH_POTION_HP, Rules.MAX_HP - h.hp)
        db.saveHero(h.copy(gold = h.gold - cost, hp = h.hp + heal))
        val id = db.insertLog(LogEntry(day = today(), kind = LogKind.POTION, gold = -cost, hp = heal))
        emit(Outcome(gold = -cost, hp = heal, logId = id))
    }

    /** All dailies due on [day] (that existed then) are done. */
    private suspend fun isPerfect(day: Long): Boolean {
        val dayStart = prefs().dayStart
        val due = db.tasks().filter { it.type == TaskType.DAILY && Rules.isDue(it, day) && existedOn(it, day, dayStart) }
        return due.isNotEmpty() && due.all { it.doneDay == day }
    }

    private fun existedOn(t: Task, day: Long, dayStart: Int) =
        Rules.logicalDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(t.createdAt), ZoneId.systemDefault()), dayStart) <= day

    /** Opens one earned chest: gold, a little XP and two or three items. */
    suspend fun openChest(): Loot? = mutex.withLock {
        val h = hero()
        if (h.chests <= 0) return null
        val season = season()
        val gold = kotlin.math.round(10 + h.level * 1.5 + random.nextInt(11))
        val xp = kotlin.math.round(Rules.xpToNext(h.level) * 0.1)
        val items = List(if (random.nextDouble() < 0.25) 3 else 2) { pickItem(h, season) }
        items.forEach { db.addItem(it, 1) }
        val next = levelUp(h.copy(chests = h.chests - 1, gold = h.gold + gold, xp = h.xp + xp))
        db.saveHero(next)
        db.insertLog(LogEntry(day = today(), kind = LogKind.CHEST, gold = gold, xp = xp))
        Loot(gold, xp, items, if (next.level > h.level) next.level else 0)
    }

    /** Opens today's morning card: hands out its gift (or the goblin's theft) once. */
    suspend fun openEvent(): Outcome? = mutex.withLock {
        val h = hero()
        val day = today()
        if (h.eventDay == day) return null
        val ev = DayEvent.of(day)
        val items = giftItems(ev.gift)
        items.forEach { db.addItem(it, 1) }
        val gold = if (ev.giftGold < 0) -minOf(-ev.giftGold, h.gold) else ev.giftGold
        val heal = minOf(ev.heal, Rules.MAX_HP - h.hp).coerceAtLeast(0.0)
        db.saveHero(h.copy(eventDay = day, gold = h.gold + gold, hp = h.hp + heal))
        if (gold != 0.0 || heal > 0) db.insertLog(LogEntry(day = day, kind = LogKind.EVENT, gold = gold, hp = heal))
        if (gold == 0.0 && heal == 0.0 && items.isEmpty()) null
        else emit(Outcome(gold = gold, hp = heal, drop = items.firstOrNull().orEmpty(), items = items))
    }

    private suspend fun giftItems(g: Gift): List<String> = when (g) {
        Gift.NONE -> emptyList()
        Gift.EGG -> listOf("egg:" + Catalog.basicEggs().let { it[random.nextInt(it.size)] }.key)
        Gift.POTION -> listOf(
            if (random.nextInt(4) == 0) "potion:" + Potion.of(season()).key
            else "potion:" + Catalog.basicPotions().let { it[random.nextInt(it.size)] }.key
        )
        Gift.FOODS -> List(3) { "food:" + Food.entries[random.nextInt(Food.entries.size)].key }
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
        // Steps back out of the dungeon, but a cleared room stays cleared.
        val run = Dungeon.parse(h.dungeon)
        val dungeon = if (e.step > 0 && run != null && Dungeon.current(h.dungeon, e.day) == run) {
            Dungeon.encode(run.copy(steps = (run.steps - e.step).coerceAtLeast(0)))
        } else h.dungeon
        db.saveHero(
            h.copy(
                xp = (h.xp - e.xp).coerceAtLeast(0.0),
                gold = (h.gold - e.gold).coerceAtLeast(0.0),
                hp = (h.hp - e.hp).coerceIn(1.0, Rules.MAX_HP),
                mp = (h.mp - e.mp).coerceAtLeast(0.0),
                questHp = questHp,
                dropsToday = if (e.drop.isNotEmpty()) (h.dropsToday - 1).coerceAtLeast(0) else h.dropsToday,
                dungeon = dungeon,
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
        val ev = DayEvent.of(day)
        val pet = boost(h)
        val buffs = Classes.buffs(h, day)
        val party = party(day)
        // Focus sessions pay like a task but don't count as one (bounties, achievements).
        val counts = kind != LogKind.FOCUS
        val doneBefore = db.doneOn(day)
        val mult = bonus * ev.reward(kind, d, isFirst = counts && doneBefore == 0) * challenge().reward
        val jackpot = ev.jackpot > 0 && random.nextDouble() < ev.jackpot
        val xp = Rules.xpGain(delta, d, s, mult) * ev.xp * (1 + pet.xp + buffs.xp + party)
        val gold = Rules.goldGain(delta, d, s, mult) * ev.gold * (if (jackpot) 5.0 else 1.0) * (1 + pet.gold + party)
        val boss = Catalog.boss(h.quest)
        val crit = boss != null && random.nextDouble() < Rules.critChance(s) + ev.crit + pet.crit + buffs.crit
        val hit = if (boss != null) Rules.bossDamage(delta, d, s, crit) * ev.boss * (1 + pet.boss) else 0.0
        val drop = rollDrop(h, d, s, season(), ev, pet.drop + buffs.drop, buffs.dropCap)
        if (drop.isNotEmpty()) db.addItem(drop, 1)
        // A healing companion (or a healer's renewal) patches the hero up a little with every finished task.
        val heal = minOf(pet.heal + buffs.heal, Rules.MAX_HP - h.hp).coerceAtLeast(0.0)
        val mp = if (Classes.of(h) == null) 0.0 else minOf(Classes.mpGain(s), Classes.maxMp(s) - h.mp).coerceAtLeast(0.0)

        var next = h.copy(xp = h.xp + xp, gold = h.gold + gold, hp = h.hp + heal, mp = h.mp + mp, questHp = h.questHp - hit, dropsToday = h.dropsToday + if (drop.isNotEmpty()) 1 else 0)
        // The card's bounty pays once, the moment the day's count reaches it (undoing later keeps it).
        val bounty = counts && ev.bounty > 0 && h.bountyDay != day && doneBefore + 1 >= ev.bounty
        val bountyItems = if (bounty) giftItems(ev.bountyGift) else emptyList()
        if (bounty) {
            bountyItems.forEach { db.addItem(it, 1) }
            next = next.copy(gold = next.gold + ev.bountyGold, bountyDay = day)
            db.insertLog(LogEntry(day = day, kind = LogKind.EVENT, gold = ev.bountyGold))
        }
        val walk = walkDungeon(next, d, day)
        val (won, defeated) = defeat(walk.hero, day)
        val before = won.level
        next = levelUp(won)
        val id = db.insertLog(LogEntry(taskId = taskId, day = day, kind = kind, xp = xp, gold = gold, hp = heal, valueDelta = delta, boss = hit, drop = drop, mp = mp, step = walk.step))
        return next to Outcome(
            xp = xp, gold = gold, hp = heal, boss = hit, crit = crit, drop = drop, levelUp = if (next.level > before) next.level else 0, bossDefeated = defeated, logId = id,
            jackpot = jackpot, bounty = bounty, bountyGold = if (bounty) ev.bountyGold else 0.0, items = bountyItems, mp = mp,
            room = walk.room, dungeonDone = walk.done, roomGold = walk.gold, roomItems = walk.items,
        )
    }

    /** A boss brought to zero: its loot, the win on record and the fight closed. */
    private suspend fun defeat(h: Hero, day: Long): Pair<Hero, Boss?> {
        val boss = Catalog.boss(h.quest) ?: return h to null
        if (h.questHp > 0) return h to null
        boss.rewards.forEach { (key, n) -> db.addItem(key, n) }
        db.insertLog(LogEntry(day = day, kind = LogKind.VICTORY, gold = boss.gold, xp = boss.xp))
        return h.copy(quest = "", questHp = 0.0, questsDone = h.withWin(boss.record), xp = h.xp + boss.xp, gold = h.gold + boss.gold) to boss
    }

    private class Walk(val hero: Hero, val step: Int = 0, val room: Int = 0, val done: Boolean = false, val gold: Double = 0.0, val items: List<String> = emptyList())

    /** On weekends every finished task is a step through the dungeon; cleared rooms pay at once. */
    private suspend fun walkDungeon(h: Hero, d: Difficulty, day: Long): Walk {
        if (!Dungeon.isOpen(day)) return Walk(h)
        val run = Dungeon.current(h.dungeon, day)
        if (run.finished) return Walk(h)
        val step = Dungeon.steps(d)
        val (after, cleared) = Dungeon.advance(run, step)
        var next = h.copy(dungeon = Dungeon.encode(after))
        if (cleared.isEmpty()) return Walk(next, step)
        var gold = 0.0
        val items = ArrayList<String>()
        cleared.forEach { room ->
            gold += Dungeon.roomGold(h.level, room)
            if (room == Dungeon.last) {
                items += Rules.SADDLE
                next = next.copy(chests = next.chests + 1, dungeonsCleared = next.dungeonsCleared + 1)
            } else items += pickItem(h, season())
        }
        items.forEach { db.addItem(it, 1) }
        // Logged on its own, so undoing the task that cleared a room keeps the room's loot.
        db.insertLog(LogEntry(day = day, kind = LogKind.DUNGEON, gold = gold))
        return Walk(next.copy(gold = next.gold + gold), step, cleared.last() + 1, Dungeon.last in cleared, gold, items)
    }

    private suspend fun hurt(h: Hero, d: Difficulty, delta: Double, taskId: Long, kind: LogKind, day: Long): Pair<Hero, Outcome> {
        val challenge = challenge()
        val dmg = Rules.damage(delta, d, stats(h)) * DayEvent.of(day).hurt * challenge.hurt * (1 - boost(h).guard) * Classes.buffs(h, day).hurt
        val id = db.insertLog(LogEntry(taskId = taskId, day = day, kind = kind, hp = -dmg, valueDelta = delta))
        val hurt = h.copy(hp = h.hp - dmg)
        return if (hurt.hp <= 0) faint(hurt, challenge) to Outcome(hp = -dmg, fainted = true, logId = id)
        else hurt to Outcome(hp = -dmg, logId = id)
    }

    private fun levelUp(h: Hero): Hero {
        var x = h
        while (x.xp >= Rules.xpToNext(x.level)) {
            x = x.copy(xp = x.xp - Rules.xpToNext(x.level), level = x.level + 1, hp = Rules.MAX_HP)
        }
        return x
    }

    /** Fainting costs a level, the purse and the boss fight's progress; gear and pets are kept. Relaxed: only half the purse. */
    private fun faint(h: Hero, c: Challenge) = if (c == Challenge.RELAXED) h.copy(
        gold = h.gold / 2, hp = Rules.MAX_HP, deaths = h.deaths + 1, questHp = Catalog.boss(h.quest)?.hp ?: 0.0,
    ) else h.copy(
        level = (h.level - 1).coerceAtLeast(1), xp = 0.0, gold = 0.0, hp = Rules.MAX_HP, deaths = h.deaths + 1,
        questHp = Catalog.boss(h.quest)?.hp ?: 0.0,
    )

    private fun rollDrop(h: Hero, d: Difficulty, s: Stats, season: Season, ev: DayEvent, petDrop: Double, extraCap: Int = 0): String {
        if (h.dropsToday >= Rules.dropCap(h.level) + ev.dropCap + extraCap) return ""
        if (random.nextDouble() >= (Rules.dropChance(d, s) + ev.drop + petDrop).coerceAtMost(0.65)) return ""
        return pickItem(h, season)
    }

    /** One random egg, hatching potion or food, with a small chance of a boss-only rarity. */
    private fun pickItem(h: Hero, season: Season): String {
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
        val dayStart = prefs().dayStart
        return db.tasks().filter { it.type == TaskType.DAILY && Rules.isDue(it, day) && it.doneDay != day && existedOn(it, day, dayStart) }
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
        val challenge = challenge()
        // Skills cast on the closing day still hold tonight.
        val buffs = Classes.buffs(h, day)
        val hurt = DayEvent.of(day).hurt * challenge.hurt * (1 - boost(h).guard) * buffs.hurt
        val tasks = db.tasks()
        val updated = tasks.mapNotNull { t ->
            when (t.type) {
                TaskType.DAILY -> if (t in missed && !h.resting) {
                    val delta = Rules.delta(t.value, false)
                    val dmg = Rules.damage(delta, t.difficulty, s) * hurt
                    hp += dmg
                    if (boss != null && !buffs.bossWard) bossHit += boss.str * t.difficulty.mult.coerceAtLeast(0.5) * 1.5 * Rules.defense(s) * hurt
                    db.insertLog(LogEntry(taskId = t.id, day = day, kind = LogKind.MISSED, hp = -dmg, valueDelta = delta))
                    t.copy(value = t.value + delta, streak = if (buffs.keepStreaks) t.streak else 0)
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
        // A night's sleep brings back some mana; spent buffs fall away.
        val mp = if (Classes.of(h) == null) h.mp else (h.mp + Classes.mpOvernight(s)).coerceAtMost(Classes.maxMp(s))
        val buffsLeft = Classes.decode(h.buffs).filterValues { it >= today }.entries.joinToString(",") { "${it.key}=${it.value}" }
        var next = h.copy(hp = h.hp - hp - bossHit, lastDay = today, dropsToday = 0, mp = mp, buffs = buffsLeft)
        val fainted = next.hp <= 0
        if (fainted) next = faint(next, challenge)
        db.saveHero(next)
        if (hp + bossHit > 0) _fx.tryEmit(Outcome(hp = -(hp + bossHit), fainted = fainted))
        // The companion wanders off overnight and sometimes brings something back.
        val found = if (h.pet.isEmpty()) "" else Companions.forage(db.pet(h.pet), random)
        if (found.isNotEmpty()) db.addItem(found, 1)
        DayReport(day, missed.size, hp, bossHit, fainted, h.resting, found)
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

    /** Buys one portion of pet food. */
    suspend fun buyFood(food: Food): Boolean = mutex.withLock {
        val h = hero()
        val cost = Rules.foodPrice(food)
        if (h.gold + 1e-9 < cost) return false
        db.saveHero(h.copy(gold = h.gold - cost))
        db.addItem("food:${food.key}", 1)
        db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -cost))
        true
    }

    /** Returns how much the pet grew, 0 when nothing happened. */
    suspend fun feed(petKey: String, food: Food): Int = mutex.withLock {
        val pet = db.pet(petKey) ?: return 0
        if (pet.growth >= 100 || (db.item("food:${food.key}")?.count ?: 0) < 1) return 0
        val species = Catalog.petParts(petKey)?.first ?: return 0
        val grow = ((if (species.favorite == food) Rules.FAVORITE_FOOD else Rules.OTHER_FOOD) * DayEvent.of(today()).feed).toInt()
        db.addItem("food:${food.key}", -1)
        db.savePet(pet.copy(growth = (pet.growth + grow).coerceAtMost(100)))
        grow
    }

    /** One pet can walk beside the hero or carry them, not both at once. */
    suspend fun setCompanion(petKey: String) = mutex.withLock {
        val h = hero()
        db.saveHero(h.copy(pet = petKey, mount = if (h.mount == petKey) "" else h.mount))
    }

    // ---- Mounts --------------------------------------------------------------------------

    suspend fun buySaddle(): Boolean = mutex.withLock {
        val h = hero()
        if (h.gold + 1e-9 < Rules.SADDLE_COST) return false
        db.saveHero(h.copy(gold = h.gold - Rules.SADDLE_COST))
        db.addItem(Rules.SADDLE, 1)
        db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -Rules.SADDLE_COST))
        true
    }

    /** Saddles a fully grown pet: it becomes a mount for good. The first one is ridden right away. */
    suspend fun saddle(petKey: String): Boolean = mutex.withLock {
        val pet = db.pet(petKey) ?: return false
        if (pet.growth < 100 || pet.mount || (db.item(Rules.SADDLE)?.count ?: 0) < 1) return false
        db.addItem(Rules.SADDLE, -1)
        db.savePet(pet.copy(mount = true))
        val h = hero()
        if (h.mount.isEmpty()) db.saveHero(h.copy(mount = petKey, pet = if (h.pet == petKey) "" else h.pet))
        true
    }

    /** Rides a saddled pet, or ("") gets off. */
    suspend fun ride(petKey: String) = mutex.withLock {
        val h = hero()
        if (petKey.isEmpty()) db.saveHero(h.copy(mount = ""))
        else if (db.pet(petKey)?.mount == true) db.saveHero(h.copy(mount = petKey, pet = if (h.pet == petKey) "" else h.pet))
    }

    // ---- Classes and skills --------------------------------------------------------------

    /** Picks a class (from level 5). The first pick is free, a change costs gold and empties the mana. */
    suspend fun chooseClass(c: HeroClass): Boolean = mutex.withLock {
        val h = hero()
        if (h.level < Classes.UNLOCK || h.heroClass == c.name) return false
        val cost = if (h.heroClass.isEmpty()) 0.0 else Classes.CHANGE_COST
        if (h.gold + 1e-9 < cost) return false
        val fresh = h.copy(heroClass = c.name, gold = h.gold - cost, buffs = "")
        db.saveHero(fresh.copy(mp = if (h.heroClass.isEmpty()) Classes.maxMp(stats(fresh)) else 0.0))
        if (cost > 0) db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -cost))
        true
    }

    /** Why a skill can't be cast right now, or null when it can. */
    fun blocked(h: Hero, skill: Skill, day: Long): SkillBlock? = when {
        Classes.of(h) != skill.cls -> SkillBlock.CLASS
        h.level < skill.level -> SkillBlock.LEVEL
        skill.kind == SkillKind.DAY && skill in Classes.active(h.buffs, day) -> SkillBlock.ACTIVE
        (skill == Skill.SMASH || skill == Skill.FIREBALL) && h.quest.isEmpty() -> SkillBlock.NO_BOSS
        skill == Skill.HEAL && h.hp >= Rules.MAX_HP -> SkillBlock.FULL_HP
        h.mp + 1e-9 < skill.mana -> SkillBlock.MANA
        else -> null
    }

    suspend fun cast(skill: Skill): Outcome? = mutex.withLock {
        val h = hero()
        val day = today()
        if (blocked(h, skill, day) != null) return null
        val s = stats(h)
        var next = h.copy(mp = h.mp - skill.mana)
        var out = Outcome(mp = -skill.mana, skill = skill)
        when (skill) {
            Skill.SMASH, Skill.FIREBALL -> {
                val hit = if (skill == Skill.SMASH) Classes.smash(s) else Classes.fireball(s)
                val xp = if (skill == Skill.FIREBALL) Classes.fireballXp(s) else 0.0
                next = next.copy(questHp = next.questHp - hit, xp = next.xp + xp)
                val (won, defeated) = defeat(next, day)
                val before = won.level
                next = levelUp(won)
                out = out.copy(boss = hit, xp = xp, bossDefeated = defeated, levelUp = if (next.level > before) next.level else 0)
            }
            Skill.PICKPOCKET -> Classes.pickpocket(s).let { g -> next = next.copy(gold = next.gold + g); out = out.copy(gold = g) }
            Skill.HEAL -> minOf(Classes.heal(s), Rules.MAX_HP - h.hp).let { hp -> next = next.copy(hp = next.hp + hp); out = out.copy(hp = hp) }
            else -> next = next.copy(buffs = Classes.encode(h.buffs, skill, day))
        }
        db.saveHero(next)
        val id = db.insertLog(LogEntry(day = day, kind = LogKind.SKILL, mp = -skill.mana, boss = out.boss, xp = out.xp, gold = out.gold, hp = out.hp))
        emit(out.copy(logId = id))
    }

    // ---- Focus ---------------------------------------------------------------------------

    /** A finished focus session pays like a task of matching size (and counts as a dungeon step). */
    suspend fun focusDone(minutes: Int): Outcome? = mutex.withLock {
        if (minutes < Rules.FOCUS_MIN) return null
        val (h2, o) = gain(hero(), Rules.focusDifficulty(minutes), 1.0, 1.0, 0, LogKind.FOCUS, today())
        db.saveHero(h2)
        emit(o)
    }

    /** The fights on offer right now: the next rank of each main boss, this season's boss and this week's. */
    suspend fun offers(h: Hero): Offers {
        val date = LocalDate.ofEpochDay(today())
        val southern = prefs().southern
        val s = Season.of(date, southern)
        val (wy, wk) = Catalog.weekOf(date)
        return Offers(
            main = Catalog.bosses.map { Catalog.nextFight(it, h.wins(it.key)) },
            seasonal = Catalog.seasonalBosses(s, Season.year(date, southern), h.level).filterNot { h.defeated(it.record) },
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

    /** Opens a new part of the home: the garden or the attic. */
    suspend fun buyArea(a: HomeArea): Boolean = mutex.withLock {
        val h = hero()
        if (a.price <= 0.0 || h.ownsDecor(a.ownedKey)) return true
        if (h.level < a.level || h.gold + 1e-9 < a.price) return false
        db.saveHero(h.copy(gold = h.gold - a.price, ownedDecor = h.ownedDecor + "," + a.ownedKey))
        db.insertLog(LogEntry(day = today(), kind = LogKind.BUY, gold = -a.price))
        true
    }

    /** Puts an owned item in a spot (or clears it with null), or applies a wallpaper / floor. */
    suspend fun place(spot: Spot?, d: Decor?) = mutex.withLock {
        val h = hero()
        if (d != null && !h.ownsDecor(d.key)) return
        when {
            spot == null && d?.type == DecorType.WALLPAPER -> db.saveHero(h.copy(wallpaper = d.key))
            spot == null && d?.type == DecorType.FLOOR -> db.saveHero(h.copy(floor = d.key))
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
        val created = Rules.logicalDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(h.createdAt), ZoneId.systemDefault()), prefs().dayStart)
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
            mounts = db.mountCount(),
            dungeons = h.dungeonsCleared,
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

    // ---- Tavern --------------------------------------------------------------------------

    /** This hero as a friend would see it. */
    suspend fun heroCard(): HeroCard {
        val h = hero().let { if (it.uid.isEmpty()) it.copy(uid = newUid()).also { u -> db.saveHero(u) } else it }
        val pets = db.pets()
        return HeroCard(
            uid = h.uid, name = h.name, level = h.level, heroClass = h.heroClass,
            skin = h.skin, hairStyle = h.hairStyle, hairColor = h.hairColor,
            weapon = h.weapon, armor = h.armor, head = h.head, shield = h.shield,
            pet = h.pet, petGrown = (pets.firstOrNull { it.key == h.pet }?.growth ?: 0) >= 100, mount = h.mount,
            bestStreak = db.bestStreak(), bosses = h.defeatedCount, pets = pets.size,
            achievements = h.achievements.split(',').count { it.isNotEmpty() }, dungeons = h.dungeonsCleared, day = today(),
        )
    }

    /** Takes a friend's code into the tavern (a newer card of the same hero replaces the old one). */
    suspend fun addFriend(text: String): Pair<FriendResult, HeroCard?> {
        val card = Party.decode(text) ?: return FriendResult.BAD to null
        if (card.uid == hero().uid) return FriendResult.SELF to card
        val old = db.friends().firstOrNull { it.uid == card.uid }
        db.saveFriend(app.dayquest.data.Friend(card.uid, Party.encode(card), old?.addedAt ?: System.currentTimeMillis()))
        return (if (old == null) FriendResult.ADDED else FriendResult.UPDATED) to card
    }

    suspend fun abandonQuest() = mutex.withLock {
        db.saveHero(hero().copy(quest = "", questHp = 0.0))
    }
}
