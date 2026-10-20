package app.dayquest

import app.dayquest.data.CheckItem
import app.dayquest.data.DayKind
import app.dayquest.data.Friend
import app.dayquest.data.JournalEntry
import app.dayquest.data.GameDao
import app.dayquest.data.Hero
import app.dayquest.data.Item
import app.dayquest.data.LogEntry
import app.dayquest.data.LogKind
import app.dayquest.data.Moment
import app.dayquest.data.Pet
import app.dayquest.data.Settings
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Catalog
import app.dayquest.game.Challenge
import app.dayquest.game.Classes
import app.dayquest.game.Difficulty
import app.dayquest.game.Dungeon
import app.dayquest.game.DungeonRun
import app.dayquest.game.HeroCard
import app.dayquest.game.HeroClass
import app.dayquest.game.Party
import app.dayquest.game.Skill
import app.dayquest.game.SkillBlock
import app.dayquest.game.Companions
import app.dayquest.game.Food
import app.dayquest.game.Perk
import app.dayquest.game.Species
import app.dayquest.game.DayEvent
import app.dayquest.game.Game
import app.dayquest.game.Rules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private fun game(db: FakeDao, challenge: Challenge = Challenge.NORMAL) = Game(db, { Settings(challenge = challenge) }, Random(1))

    private suspend fun start(db: FakeDao, g: Game) = db.saveHero(Hero(name = "x", lastDay = g.today()))

    @Test
    fun thirtyDaysInARowNeverRepeatACard() {
        assertEquals(30, DayEvent.entries.size)
        (20000L..20400L).forEach { start ->
            assertEquals(30, (start until start + 30).map { DayEvent.of(it) }.distinct().size)
        }
        assertEquals(DayEvent.of(20555), DayEvent.of(20555))
    }

    @Test
    fun undoingADailyRestoresItsColour() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        start(db, g)
        val id = db.insert(Task(type = TaskType.DAILY, title = "d", value = 0.5))
        val o = g.setDaily(id, true)!!
        g.undo(o.logId)
        assertEquals(0.5, db.task(id)!!.value, 1e-9)
        assertEquals(3, Rules.band(db.task(id)!!.value))
    }

    @Test
    fun aPerfectDayPaysOneChest() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        start(db, g)
        val a = db.insert(Task(type = TaskType.DAILY, title = "a"))
        val b = db.insert(Task(type = TaskType.DAILY, title = "b"))
        assertFalse(g.setDaily(a, true)!!.perfect)
        assertTrue(g.setDaily(b, true)!!.perfect)
        assertEquals(1, db.hero!!.chests)
        // Unticking and ticking again the same day pays nothing more.
        g.setDaily(b, false)
        assertFalse(g.setDaily(b, true)!!.perfect)
        assertEquals(1, db.hero!!.chests)

        val gold = db.hero!!.gold
        val itemsBefore = db.items.values.sumOf { it.count }
        val loot = g.openChest()!!
        assertTrue(loot.gold > 0 && loot.items.size >= 2)
        assertEquals(0, db.hero!!.chests)
        assertEquals(gold + loot.gold, db.hero!!.gold, 1e-9)
        assertEquals(itemsBefore + loot.items.size, db.items.values.sumOf { it.count })
        assertNull(g.openChest())
    }

    @Test
    fun aBountyPaysOnceWhenTheCountIsReached() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        start(db, g)
        val day = (0L until 30L).map { g.today() - 30 + it }.first { DayEvent.of(it) == DayEvent.TREASURE_MAP }
        val ids = (1..6).map { db.insert(Task(type = TaskType.DAILY, title = "t$it")) }
        val paid = ids.map { g.setDaily(it, true, day)!!.bounty }
        assertEquals(listOf(false, false, false, false, true, false), paid)
        assertEquals(day, db.hero!!.bountyDay)
    }

    @Test
    fun relaxedHalvesDamageAndHeroicPaysMore() = runBlocking {
        fun hpLoss(c: Challenge) = runBlocking {
            val db = FakeDao()
            val g = game(db, c)
            start(db, g)
            val id = db.insert(Task(type = TaskType.HABIT, title = "h", up = false, down = true))
            -g.scoreHabit(id, false)!!.hp
        }
        assertEquals(0.5, hpLoss(Challenge.RELAXED) / hpLoss(Challenge.NORMAL), 1e-9)
        assertEquals(1.5, hpLoss(Challenge.HEROIC) / hpLoss(Challenge.NORMAL), 1e-9)

        fun xp(c: Challenge) = runBlocking {
            val db = FakeDao()
            val g = Game(db, { Settings(challenge = c) }, Random(5))
            start(db, g)
            val id = db.insert(Task(type = TaskType.HABIT, title = "h"))
            g.scoreHabit(id, true)!!.xp
        }
        assertEquals(1.25, xp(Challenge.HEROIC) / xp(Challenge.NORMAL), 1e-9)
    }

    @Test
    fun theMorningCardOpensOnce() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        start(db, g)
        g.openEvent()
        assertEquals(g.today(), db.hero!!.eventDay)
        val after = db.hero
        assertNull(g.openEvent())
        assertEquals(after, db.hero)
    }

    @Test
    fun foodCanBeBought() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today(), gold = 10.0))
        assertTrue(g.buyFood(Food.FISH))
        assertEquals(10.0 - Rules.foodPrice(Food.FISH), db.hero!!.gold, 1e-9)
        assertEquals(1, db.items["food:fish"]!!.count)
        assertFalse(g.buyFood(Food.CAKE))
    }

    @Test
    fun aGrownCompanionHelpsTwiceAsMuch() {
        val young = Pet("fox:base", growth = 40)
        assertEquals(Perk.GOLD, Companions.perk(Species.FOX))
        assertEquals(0.05, Companions.boost(young).gold, 1e-9)
        assertEquals(0.10, Companions.boost(young.copy(growth = 100)).gold, 1e-9)
        assertEquals(0.0, Companions.boost(null).gold, 1e-9)
    }

    @Test
    fun aFoxCompanionEarnsMoreGold() = runBlocking {
        fun earned(pet: String) = runBlocking {
            val db = FakeDao()
            val g = game(db)
            db.saveHero(Hero(name = "x", lastDay = g.today(), pet = pet))
            db.savePet(Pet("fox:base", growth = 100))
            val id = db.insert(Task(type = TaskType.TODO, title = "t"))
            g.setTodo(id, true)!!.gold
        }
        assertEquals(earned("") * 1.10, earned("fox:base"), 1e-9)
    }

    @Test
    fun aClassDoublesItsStatAndItsSkillsCostMana() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today(), level = 4))
        assertFalse(g.chooseClass(HeroClass.WARRIOR))
        db.saveHero(db.hero!!.copy(level = 10, gold = 50.0))
        val before = g.stats(db.hero!!)
        assertTrue(g.chooseClass(HeroClass.WARRIOR))
        assertEquals(before.str + 5, g.stats(db.hero!!).str)
        assertEquals(before.int, g.stats(db.hero!!).int)
        // The first pick is free and starts with a full pool of mana.
        assertEquals(50.0, db.hero!!.gold, 1e-9)
        assertEquals(Classes.maxMp(g.stats(db.hero!!)), db.hero!!.mp, 1e-9)

        // Smash needs a boss.
        assertEquals(SkillBlock.NO_BOSS, g.blocked(db.hero!!, Skill.SMASH, g.today()))
        assertNull(g.cast(Skill.SMASH))
        g.startQuest(Catalog.bosses.first())
        val mp = db.hero!!.mp
        val o = g.cast(Skill.SMASH)!!
        assertTrue(o.boss > 0)
        assertEquals(Catalog.bosses.first().hp - o.boss, db.hero!!.questHp, 1e-9)
        assertEquals(mp - Skill.SMASH.mana, db.hero!!.mp, 1e-9)

        // A day skill holds once a day; a change of class costs gold the hero doesn't have.
        assertNotNull(g.cast(Skill.BATTLE_CRY))
        assertEquals(SkillBlock.ACTIVE, g.blocked(db.hero!!, Skill.BATTLE_CRY, g.today()))
        assertFalse(g.chooseClass(HeroClass.MAGE))
    }

    @Test
    fun ironSkinHalvesTonightsDamage() = runBlocking {
        fun lost(cast: Boolean) = runBlocking {
            val db = FakeDao()
            val g = game(db)
            val today = g.today()
            db.saveHero(Hero(name = "x", lastDay = today - 1, level = 12, heroClass = HeroClass.WARRIOR.name, mp = 40.0))
            db.insert(Task(type = TaskType.DAILY, title = "d", streak = 5, createdAt = 0))
            if (cast) db.saveHero(db.hero!!.copy(buffs = Classes.encode("", Skill.IRON_SKIN, today - 1)))
            g.startNewDay()!!.hp
        }
        assertEquals(0.5, lost(true) / lost(false), 1e-9)
    }

    @Test
    fun shadowStepKeepsStreaksAndManaComesBackOvernight() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        val today = g.today()
        db.saveHero(Hero(name = "x", lastDay = today - 1, level = 12, heroClass = HeroClass.ROGUE.name, buffs = Classes.encode("", Skill.SHADOW_STEP, today - 1)))
        val id = db.insert(Task(type = TaskType.DAILY, title = "d", streak = 5, createdAt = 0))
        g.startNewDay()
        assertEquals(5, db.task(id)!!.streak)
        assertTrue(db.hero!!.mp > 0)
        assertEquals("", db.hero!!.buffs)
    }

    @Test
    fun manaComesWithTasksAndGoesWithUndo() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today(), level = 10, heroClass = HeroClass.MAGE.name))
        val id = db.insert(Task(type = TaskType.TODO, title = "t"))
        val o = g.setTodo(id, true)!!
        assertTrue(o.mp > 0)
        assertEquals(o.mp, db.hero!!.mp, 1e-9)
        g.undo(o.logId)
        assertEquals(0.0, db.hero!!.mp, 1e-9)
    }

    @Test
    fun theDungeonWalksRoomByRoom() {
        val run = DungeonRun(2026, 42)
        val (a, cleared) = Dungeon.advance(run, 1)
        assertTrue(cleared.isEmpty())
        assertEquals(1, a.steps)
        val (b, cleared2) = Dungeon.advance(a, 5)
        assertEquals(listOf(0, 1), cleared2)
        assertEquals(2, b.room)
        assertEquals(1, b.steps)
        val (c, _) = Dungeon.advance(b, 100)
        assertTrue(c.finished)
        assertEquals(c, Dungeon.parse(Dungeon.encode(c)))
        assertEquals(Dungeon.ROOMS.size, Dungeon.foes(2026, 42).size)
        assertEquals(Dungeon.foes(2026, 42), Dungeon.foes(2026, 42))
        // Saturday 2026-10-17 and Sunday are open; the Monday after is not.
        val sat = java.time.LocalDate.of(2026, 10, 17).toEpochDay()
        assertTrue(Dungeon.isOpen(sat) && Dungeon.isOpen(sat + 1) && !Dungeon.isOpen(sat + 2))
        assertEquals(5, Dungeon.daysUntilOpen(sat + 2))
        // A new week starts a fresh run.
        assertEquals(0, Dungeon.current(Dungeon.encode(c), sat + 7).room)
    }

    @Test
    fun aGrownPetCanBeSaddledAndRidden() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        db.saveHero(Hero(name = "x", lastDay = g.today(), gold = 100.0, pet = "wolf:base"))
        db.savePet(Pet("wolf:base", growth = 100))
        db.savePet(Pet("fox:base", growth = 50))
        assertFalse(g.saddle("wolf:base"))
        assertTrue(g.buySaddle())
        assertFalse(g.saddle("fox:base"))
        assertTrue(g.saddle("wolf:base"))
        // Riding the companion sends it off the walking spot.
        assertEquals("wolf:base", db.hero!!.mount)
        assertEquals("", db.hero!!.pet)
        assertEquals(0, db.items[Rules.SADDLE]!!.count)
        assertEquals(0.15, Companions.mountBoost(db.pets["wolf:base"]).boss, 1e-9)
        g.setCompanion("wolf:base")
        assertEquals("", db.hero!!.mount)
    }

    @Test
    fun heroCodesTravelAndFreshFriendsHelp() = runBlocking {
        val card = HeroCard("abc123", "Ann|e", 12, "MAGE", 1, 2, 3, "iron_sword", "tunic", "", "", "fox:base", true, "", 30, 4, 9, 12, 1, 20400)
        val code = Party.encode(card)
        assertTrue(code.startsWith(Party.PREFIX))
        assertEquals(card, Party.decode("Hi! Here is my hero: $code see you"))
        assertNull(Party.decode("DQ1:notahero"))
        assertEquals(0.02, Party.bonus(listOf(card), 20405), 1e-9)
        assertEquals(0.0, Party.bonus(listOf(card), 20410), 1e-9)

        fun earned(friends: Int) = runBlocking {
            val db = FakeDao()
            val g = game(db)
            start(db, g)
            repeat(friends) { db.saveFriend(Friend("f$it", Party.encode(card.copy(uid = "f$it", day = g.today())))) }
            val id = db.insert(Task(type = TaskType.TODO, title = "t"))
            g.setTodo(id, true)!!.gold
        }
        assertEquals(earned(0) * 1.10, earned(7), 1e-9)
    }

    @Test
    fun aFocusSessionPaysButIsNotATask() = runBlocking {
        val db = FakeDao()
        val g = game(db)
        start(db, g)
        assertNull(g.focusDone(5))
        val o = g.focusDone(25)!!
        assertTrue(o.xp > 0 && o.gold > 0)
        assertEquals(0, db.doneOn(g.today()))
        assertEquals(Difficulty.HARD, Rules.focusDifficulty(50))
    }

    @Test
    fun anOwlCompanionCanBringFoodOvernight() {
        val r = Random(3)
        val finds = List(200) { Companions.forage(Pet("owl:base", growth = 100), r) }.filter { it.isNotEmpty() }
        assertTrue(finds.size in 90..150)
        assertTrue(finds.all { it.startsWith("food:") })
        assertTrue(finds.count { it == "food:meat" } > finds.size / 2)
    }
}

/** An in-memory stand-in for Room, enough for the game rules. */
internal class FakeDao : GameDao() {
    val tasks = linkedMapOf<Long, Task>()
    val log = linkedMapOf<Long, LogEntry>()
    var hero: Hero? = null
    val items = mutableMapOf<String, Item>()
    val pets = mutableMapOf<String, Pet>()
    val friends = mutableMapOf<String, Friend>()
    private var next = 1L

    override fun observeTasks(): Flow<List<Task>> = TODO()
    override suspend fun tasks() = tasks.values.toList()
    override suspend fun task(id: Long) = tasks[id]
    override fun observeTask(id: Long): Flow<Task?> = TODO()
    override suspend fun nextPosition(type: TaskType) = 0
    override suspend fun insert(task: Task): Long { val id = next++; tasks[id] = task.copy(id = id); return id }
    override suspend fun update(task: Task) { tasks[task.id] = task }
    override suspend fun updateTasks(tasks: List<Task>) = tasks.forEach { update(it) }
    override suspend fun delete(task: Task) { tasks.remove(task.id) }
    override fun observeChecklist(): Flow<List<CheckItem>> = TODO()
    override suspend fun checklist(taskId: Long) = emptyList<CheckItem>()
    override suspend fun insertCheck(item: CheckItem) = 0L
    override suspend fun updateCheck(item: CheckItem) = Unit
    override suspend fun deleteCheck(item: CheckItem) = Unit
    override suspend fun clearChecklist(taskId: Long) = Unit
    override suspend fun resetDailyChecklists() = Unit
    override suspend fun insertLog(entry: LogEntry): Long { val id = next++; log[id] = entry.copy(id = id); return id }
    override suspend fun updateLog(entry: LogEntry) { log[entry.id] = entry }
    override suspend fun logEntry(id: Long) = log[id]
    override suspend fun lastLog(taskId: Long, kind: LogKind) = log.values.filter { it.taskId == taskId && it.kind == kind && !it.undone }.maxByOrNull { it.id }
    override fun observeLogSince(from: Long): Flow<List<LogEntry>> = TODO()
    override fun observeDoneCount(): Flow<Int> = TODO()
    override suspend fun doneCount() = 0
    override suspend fun doneOn(day: Long) = log.values.count { !it.undone && it.day == day && it.kind in DONE }
    override fun observeDoneOn(day: Long): Flow<Int> = TODO()
    override suspend fun activeDays() = 0
    override suspend fun bestStreak() = 0
    override fun observeHistory(): Flow<List<DayKind>> = TODO()
    override fun observeHero(): Flow<Hero?> = TODO()
    override suspend fun hero() = hero
    override suspend fun saveHero(hero: Hero) { this.hero = hero }
    override fun observeItems(): Flow<List<Item>> = TODO()
    override suspend fun item(key: String) = items[key]
    override suspend fun saveItem(item: Item) { items[item.key] = item }
    override fun observePets(): Flow<List<Pet>> = TODO()
    override suspend fun pet(key: String): Pet? = pets[key]
    override suspend fun pets() = pets.values.toList()
    override suspend fun savePet(pet: Pet) { pets[pet.key] = pet }
    override suspend fun allChecklist() = emptyList<CheckItem>()
    override suspend fun allLog() = log.values.toList()
    override suspend fun allItems() = items.values.toList()
    override suspend fun allPets() = emptyList<Pet>()
    override suspend fun clearTasks() = Unit
    override suspend fun clearLog() = Unit
    override suspend fun clearItems() = Unit
    override suspend fun clearPets() = Unit
    override suspend fun clearHero() = Unit
    override suspend fun insertTasks(items: List<Task>) = Unit
    override suspend fun insertChecklist(items: List<CheckItem>) = Unit
    override suspend fun insertLogs(items: List<LogEntry>) = Unit
    override suspend fun insertItems(items: List<Item>) = Unit
    override suspend fun insertPets(items: List<Pet>) = Unit
    override fun observeTaskLog(taskId: Long): Flow<List<LogEntry>> = TODO()
    override fun observeTaskEventsSince(from: Long): Flow<List<LogEntry>> = TODO()
    override suspend fun mountCount() = pets.values.count { it.mount }
    override fun observeJournal(): Flow<List<JournalEntry>> = TODO()
    override fun observeJournalDay(day: Long): Flow<JournalEntry?> = TODO()
    override suspend fun saveJournal(entry: JournalEntry) = Unit
    override suspend fun deleteJournal(day: Long) = Unit
    override fun observeFriends(): Flow<List<Friend>> = TODO()
    override suspend fun friends() = friends.values.toList()
    override suspend fun saveFriend(friend: Friend) { friends[friend.uid] = friend }
    override suspend fun deleteFriend(uid: String) { friends.remove(uid) }
    override suspend fun allJournal() = emptyList<JournalEntry>()
    override suspend fun clearJournal() = Unit
    override suspend fun clearFriends() = Unit
    override suspend fun insertJournal(items: List<JournalEntry>) = Unit
    override suspend fun insertFriends(items: List<Friend>) = Unit
    val moments = mutableListOf<Moment>()
    override fun observeMoments(): Flow<List<Moment>> = TODO()
    override suspend fun lastMoment(kind: String) = moments.lastOrNull { it.kind == kind }
    override suspend fun insertMoment(m: Moment): Long { moments += m.copy(id = next++); return next - 1 }
    override suspend fun allMoments() = moments.toList()
    override suspend fun clearMoments() = moments.clear()
    override suspend fun insertMoments(items: List<Moment>) { moments += items }

    private companion object {
        val DONE = setOf(LogKind.UP, LogKind.DAILY, LogKind.TODO)
    }
}
