package app.dayquest

import app.dayquest.data.CheckItem
import app.dayquest.data.DayKind
import app.dayquest.data.GameDao
import app.dayquest.data.Hero
import app.dayquest.data.Item
import app.dayquest.data.LogEntry
import app.dayquest.data.LogKind
import app.dayquest.data.Pet
import app.dayquest.data.Settings
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Challenge
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
    fun anOwlCompanionCanBringFoodOvernight() {
        val r = Random(3)
        val finds = List(200) { Companions.forage(Pet("owl:base", growth = 100), r) }.filter { it.isNotEmpty() }
        assertTrue(finds.size in 90..150)
        assertTrue(finds.all { it.startsWith("food:") })
        assertTrue(finds.count { it == "food:meat" } > finds.size / 2)
    }
}

/** An in-memory stand-in for Room, enough for the game rules. */
private class FakeDao : GameDao() {
    val tasks = linkedMapOf<Long, Task>()
    val log = linkedMapOf<Long, LogEntry>()
    var hero: Hero? = null
    val items = mutableMapOf<String, Item>()
    val pets = mutableMapOf<String, Pet>()
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

    private companion object {
        val DONE = setOf(LogKind.UP, LogKind.DAILY, LogKind.TODO)
    }
}
