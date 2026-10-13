package app.dayquest.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.dayquest.game.Difficulty
import kotlinx.coroutines.flow.Flow

enum class TaskType { HABIT, DAILY, TODO, REWARD }

/** Days are "logical" epoch days: they roll over at the user's day-start hour, not at midnight. */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TaskType,
    val title: String,
    val notes: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** Habitica-style task value: grows when done, sinks when missed. Drives colour and reward size. */
    val value: Double = 0.0,
    // Habit
    val up: Boolean = true,
    val down: Boolean = false,
    val countUp: Int = 0,
    val countDown: Int = 0,
    // Daily: weekdays bitmask (Mon = bit 0) when [everyDays] is 0, otherwise every N days from [startDay].
    val repeatMask: Int = 0b1111111,
    val everyDays: Int = 0,
    val startDay: Long = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** Logical day of the last completion, -1 when never. */
    val doneDay: Long = -1,
    // To-do
    val done: Boolean = false,
    val doneAt: Long = 0,
    /** Logical day it is due, -1 when no due date. */
    val dueDay: Long = -1,
    // Reward
    val cost: Double = 10.0,
    /** Minutes after midnight for a reminder, -1 when none. */
    val reminder: Int = -1,
)

@Entity(
    tableName = "checklist",
    foreignKeys = [ForeignKey(entity = Task::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId")],
)
data class CheckItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val text: String,
    val done: Boolean = false,
    val position: Int = 0,
)

enum class LogKind { UP, DOWN, DAILY, TODO, REWARD, BUY, MISSED, BOSS, POTION, VICTORY, ACHIEVEMENT, CHEST, EVENT, SKILL, FOCUS, DUNGEON }

/**
 * One scored action and exactly what it changed, so it can be undone to the cent.
 * [taskId] is kept without a foreign key: history outlives deleted tasks.
 */
@Entity(tableName = "log", indices = [Index("day"), Index("taskId")])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val day: Long,
    val kind: LogKind,
    val xp: Double = 0.0,
    val gold: Double = 0.0,
    val hp: Double = 0.0,
    val valueDelta: Double = 0.0,
    val boss: Double = 0.0,
    /** Inventory key of a drop this action gave, or "". */
    val drop: String = "",
    val undone: Boolean = false,
    /** Mana this action gave (or, negative, spent). */
    @ColumnInfo(defaultValue = "0")
    val mp: Double = 0.0,
    /** Steps it took through the weekend dungeon. */
    @ColumnInfo(defaultValue = "0")
    val step: Int = 0,
)

/** The one and only hero (id 1). Sets are stored as comma-separated keys. */
@Entity(tableName = "hero")
data class Hero(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val level: Int = 1,
    val xp: Double = 0.0,
    val hp: Double = 50.0,
    val gold: Double = 0.0,
    val lastDay: Long = 0,
    val skin: Int = 1,
    val hairStyle: Int = 0,
    val hairColor: Int = 1,
    val weapon: String = "wooden_sword",
    val armor: String = "tunic",
    val head: String = "",
    val shield: String = "",
    val pet: String = "",
    val scene: String = "meadow",
    val ownedGear: String = "wooden_sword,tunic",
    val ownedScenes: String = "meadow",
    val quest: String = "",
    val questHp: Double = 0.0,
    /** Wins per record key: "slime_king=3,week:2026:42=1,season:winter:2026=1" (old saves: bare keys = 1 win). */
    val questsDone: String = "",
    val dropsToday: Int = 0,
    val deaths: Int = 0,
    val resting: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** Earned achievement keys, "STREAK:30,...". */
    @ColumnInfo(defaultValue = "")
    val achievements: String = "",
    /** Furniture placement, "WALL_L=clock,RUG=rug_red". */
    @ColumnInfo(defaultValue = "")
    val home: String = "",
    @ColumnInfo(defaultValue = "wp_plain,fl_planks")
    val ownedDecor: String = "wp_plain,fl_planks",
    @ColumnInfo(defaultValue = "wp_plain")
    val wallpaper: String = "wp_plain",
    @ColumnInfo(defaultValue = "fl_planks")
    val floor: String = "fl_planks",
    /** The last day every daily was done: a perfect day pays one chest. */
    @ColumnInfo(defaultValue = "-1")
    val perfectDay: Long = -1,
    /** Chests earned but not opened yet. */
    @ColumnInfo(defaultValue = "0")
    val chests: Int = 0,
    /** The day whose morning card was opened (its gift handed out). */
    @ColumnInfo(defaultValue = "-1")
    val eventDay: Long = -1,
    /** The day whose card bounty was paid. */
    @ColumnInfo(defaultValue = "-1")
    val bountyDay: Long = -1,
    /** [app.dayquest.game.HeroClass] name, "" until one is chosen. */
    @ColumnInfo(defaultValue = "")
    val heroClass: String = "",
    @ColumnInfo(defaultValue = "0")
    val mp: Double = 0.0,
    /** Skills cast on a day, "battle_cry=20410,keen_eye=20410": active until that day closes. */
    @ColumnInfo(defaultValue = "")
    val buffs: String = "",
    /** The pet the hero rides, "" when none. */
    @ColumnInfo(defaultValue = "")
    val mount: String = "",
    /** The weekend dungeon run, "year:week:room:steps"; "" when none started. */
    @ColumnInfo(defaultValue = "")
    val dungeon: String = "",
    @ColumnInfo(defaultValue = "0")
    val dungeonsCleared: Int = 0,
    /** A random id that tells this hero apart in friends' taverns. */
    @ColumnInfo(defaultValue = "")
    val uid: String = "",
) {
    fun owns(gear: String) = gear in ownedGear.split(',')
    fun ownsScene(key: String) = key in ownedScenes.split(',')
    fun ownsDecor(key: String) = key in ownedDecor.split(',')

    val wins: Map<String, Int> get() = questsDone.split(',').filter { it.isNotEmpty() }.associate { e ->
        val k = e.substringBefore('=')
        k to (e.substringAfter('=', "1").toIntOrNull() ?: 1)
    }

    fun wins(record: String) = wins[record] ?: 0
    fun defeated(record: String) = wins(record) > 0
    val defeatedCount get() = wins.values.sum()

    fun withWin(record: String): String = (wins + (record to wins(record) + 1)).entries.joinToString(",") { "${it.key}=${it.value}" }
}

/** Eggs, hatching potions and food: "egg:fox", "potion:frost", "food:berry". */
@Entity(tableName = "items")
data class Item(@PrimaryKey val key: String, val count: Int)

/** A hatched pet, "fox:frost". Growth 0..100; at 100 it is fully grown. */
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey val key: String,
    val growth: Int = 0,
    val hatchedAt: Long = System.currentTimeMillis(),
    /** Saddled: a fully grown pet that can now be ridden. */
    @ColumnInfo(defaultValue = "0")
    val mount: Boolean = false,
)

/** One line in the journal: how the day felt (1 hard .. 5 great) and a short note. */
@Entity(tableName = "journal")
data class JournalEntry(@PrimaryKey val day: Long, val mood: Int, val note: String = "")

/** A friend's hero card ([code] is their "DQH1:" hero code), shown in the tavern. */
@Entity(tableName = "friends")
data class Friend(@PrimaryKey val uid: String, val code: String, val addedAt: Long = System.currentTimeMillis())

data class Snapshot(
    val hero: Hero?,
    val tasks: List<Task>,
    val checklist: List<CheckItem>,
    val log: List<LogEntry>,
    val items: List<Item>,
    val pets: List<Pet>,
    val journal: List<JournalEntry> = emptyList(),
    val friends: List<Friend> = emptyList(),
)

@Dao
abstract class GameDao {
    // Tasks
    @Query("SELECT * FROM tasks ORDER BY position, createdAt")
    abstract fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY position, createdAt")
    abstract suspend fun tasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract suspend fun task(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract fun observeTask(id: Long): Flow<Task?>

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM tasks WHERE type = :type")
    abstract suspend fun nextPosition(type: TaskType): Int

    @Insert
    abstract suspend fun insert(task: Task): Long

    @Update
    abstract suspend fun update(task: Task)

    @Update
    abstract suspend fun updateTasks(tasks: List<Task>)

    @Delete
    abstract suspend fun delete(task: Task)

    // Checklists
    @Query("SELECT * FROM checklist ORDER BY position, id")
    abstract fun observeChecklist(): Flow<List<CheckItem>>

    @Query("SELECT * FROM checklist WHERE taskId = :taskId ORDER BY position, id")
    abstract suspend fun checklist(taskId: Long): List<CheckItem>

    @Insert
    abstract suspend fun insertCheck(item: CheckItem): Long

    @Update
    abstract suspend fun updateCheck(item: CheckItem)

    @Delete
    abstract suspend fun deleteCheck(item: CheckItem)

    @Query("DELETE FROM checklist WHERE taskId = :taskId")
    abstract suspend fun clearChecklist(taskId: Long)

    @Query("UPDATE checklist SET done = 0 WHERE taskId IN (SELECT id FROM tasks WHERE type = 'DAILY')")
    abstract suspend fun resetDailyChecklists()

    // Log
    @Insert
    abstract suspend fun insertLog(entry: LogEntry): Long

    @Update
    abstract suspend fun updateLog(entry: LogEntry)

    @Query("SELECT * FROM log WHERE id = :id")
    abstract suspend fun logEntry(id: Long): LogEntry?

    /** The latest live entry for a task on a day: what unticking a daily or to-do reverts. */
    @Query("SELECT * FROM log WHERE taskId = :taskId AND kind = :kind AND undone = 0 ORDER BY id DESC LIMIT 1")
    abstract suspend fun lastLog(taskId: Long, kind: LogKind): LogEntry?

    @Query("SELECT * FROM log WHERE undone = 0 AND day >= :from ORDER BY day")
    abstract fun observeLogSince(from: Long): Flow<List<LogEntry>>

    @Query("SELECT COUNT(*) FROM log WHERE undone = 0 AND kind IN ('UP', 'DAILY', 'TODO')")
    abstract fun observeDoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM log WHERE undone = 0 AND kind IN ('UP', 'DAILY', 'TODO')")
    abstract suspend fun doneCount(): Int

    /** Tasks finished on one day: the morning card's bounties count these. */
    @Query("SELECT COUNT(*) FROM log WHERE undone = 0 AND day = :day AND kind IN ('UP', 'DAILY', 'TODO')")
    abstract suspend fun doneOn(day: Long): Int

    @Query("SELECT COUNT(*) FROM log WHERE undone = 0 AND day = :day AND kind IN ('UP', 'DAILY', 'TODO')")
    abstract fun observeDoneOn(day: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT day) FROM log WHERE undone = 0 AND kind IN ('UP', 'DAILY', 'TODO')")
    abstract suspend fun activeDays(): Int

    @Query("SELECT COALESCE(MAX(bestStreak), 0) FROM tasks")
    abstract suspend fun bestStreak(): Int

    /** Everything the yearly chronicle needs: one row per finished task or victory. */
    @Query("SELECT day, kind FROM log WHERE undone = 0 AND kind IN ('UP', 'DAILY', 'TODO', 'VICTORY')")
    abstract fun observeHistory(): Flow<List<DayKind>>

    // Hero
    @Query("SELECT * FROM hero WHERE id = 1")
    abstract fun observeHero(): Flow<Hero?>

    @Query("SELECT * FROM hero WHERE id = 1")
    abstract suspend fun hero(): Hero?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveHero(hero: Hero)

    // Inventory
    @Query("SELECT * FROM items WHERE count > 0 ORDER BY `key`")
    abstract fun observeItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE `key` = :key")
    abstract suspend fun item(key: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveItem(item: Item)

    suspend fun addItem(key: String, n: Int) {
        val have = item(key)?.count ?: 0
        saveItem(Item(key, (have + n).coerceAtLeast(0)))
    }

    // Pets
    @Query("SELECT * FROM pets ORDER BY hatchedAt")
    abstract fun observePets(): Flow<List<Pet>>

    @Query("SELECT * FROM pets WHERE `key` = :key")
    abstract suspend fun pet(key: String): Pet?

    @Query("SELECT * FROM pets")
    abstract suspend fun pets(): List<Pet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun savePet(pet: Pet)

    /** Everything that ever happened to one task, oldest first: its statistics. */
    @Query("SELECT * FROM log WHERE taskId = :taskId AND undone = 0 ORDER BY id")
    abstract fun observeTaskLog(taskId: Long): Flow<List<LogEntry>>

    /** Finished, slipped and missed tasks since a day: the insights. */
    @Query("SELECT * FROM log WHERE undone = 0 AND day >= :from AND kind IN ('UP', 'DOWN', 'DAILY', 'TODO', 'MISSED')")
    abstract fun observeTaskEventsSince(from: Long): Flow<List<LogEntry>>

    @Query("SELECT COUNT(*) FROM pets WHERE mount = 1")
    abstract suspend fun mountCount(): Int

    // Journal
    @Query("SELECT * FROM journal ORDER BY day DESC")
    abstract fun observeJournal(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal WHERE day = :day")
    abstract fun observeJournalDay(day: Long): Flow<JournalEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveJournal(entry: JournalEntry)

    @Query("DELETE FROM journal WHERE day = :day")
    abstract suspend fun deleteJournal(day: Long)

    // Friends
    @Query("SELECT * FROM friends ORDER BY addedAt")
    abstract fun observeFriends(): Flow<List<Friend>>

    @Query("SELECT * FROM friends")
    abstract suspend fun friends(): List<Friend>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveFriend(friend: Friend)

    @Query("DELETE FROM friends WHERE uid = :uid")
    abstract suspend fun deleteFriend(uid: String)

    // Backup
    @Query("SELECT * FROM journal")
    protected abstract suspend fun allJournal(): List<JournalEntry>

    @Query("DELETE FROM journal")
    protected abstract suspend fun clearJournal()

    @Query("DELETE FROM friends")
    protected abstract suspend fun clearFriends()

    @Insert
    protected abstract suspend fun insertJournal(items: List<JournalEntry>)

    @Insert
    protected abstract suspend fun insertFriends(items: List<Friend>)

    @Query("SELECT * FROM checklist")
    protected abstract suspend fun allChecklist(): List<CheckItem>

    @Query("SELECT * FROM log")
    protected abstract suspend fun allLog(): List<LogEntry>

    @Query("SELECT * FROM items")
    protected abstract suspend fun allItems(): List<Item>

    @Query("SELECT * FROM pets")
    protected abstract suspend fun allPets(): List<Pet>

    @Query("DELETE FROM tasks")
    protected abstract suspend fun clearTasks()

    @Query("DELETE FROM log")
    protected abstract suspend fun clearLog()

    @Query("DELETE FROM items")
    protected abstract suspend fun clearItems()

    @Query("DELETE FROM pets")
    protected abstract suspend fun clearPets()

    @Query("DELETE FROM hero")
    protected abstract suspend fun clearHero()

    @Insert
    protected abstract suspend fun insertTasks(items: List<Task>)

    @Insert
    protected abstract suspend fun insertChecklist(items: List<CheckItem>)

    @Insert
    protected abstract suspend fun insertLogs(items: List<LogEntry>)

    @Insert
    protected abstract suspend fun insertItems(items: List<Item>)

    @Insert
    protected abstract suspend fun insertPets(items: List<Pet>)

    @Transaction
    open suspend fun snapshot() = Snapshot(hero(), tasks(), allChecklist(), allLog(), allItems(), allPets(), allJournal(), friends())

    /** Replaces everything with a backup (checklists cascade away with their tasks). */
    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearTasks(); clearLog(); clearItems(); clearPets(); clearHero(); clearJournal(); clearFriends()
        s.hero?.let { saveHero(it) }
        insertTasks(s.tasks)
        insertChecklist(s.checklist)
        insertLogs(s.log)
        insertItems(s.items)
        insertPets(s.pets)
        insertJournal(s.journal)
        insertFriends(s.friends)
    }
}

data class DayKind(val day: Long, val kind: LogKind)

@Database(
    entities = [Task::class, CheckItem::class, LogEntry::class, Hero::class, Item::class, Pet::class, JournalEntry::class, Friend::class],
    version = 4,
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun game(): GameDao

    companion object {
        fun create(context: Context): GameDatabase =
            Room.databaseBuilder(context.applicationContext, GameDatabase::class.java, "dayquest.db")
                .addMigrations(ACHIEVEMENTS_AND_HOME, CHESTS_AND_EVENTS, CLASSES_AND_FRIENDS)
                .build()

        private val ACHIEVEMENTS_AND_HOME = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hero ADD COLUMN achievements TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hero ADD COLUMN home TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hero ADD COLUMN ownedDecor TEXT NOT NULL DEFAULT 'wp_plain,fl_planks'")
                db.execSQL("ALTER TABLE hero ADD COLUMN wallpaper TEXT NOT NULL DEFAULT 'wp_plain'")
                db.execSQL("ALTER TABLE hero ADD COLUMN floor TEXT NOT NULL DEFAULT 'fl_planks'")
            }
        }

        private val CHESTS_AND_EVENTS = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hero ADD COLUMN perfectDay INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE hero ADD COLUMN chests INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hero ADD COLUMN eventDay INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE hero ADD COLUMN bountyDay INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val CLASSES_AND_FRIENDS = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hero ADD COLUMN heroClass TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hero ADD COLUMN mp REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hero ADD COLUMN buffs TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hero ADD COLUMN mount TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hero ADD COLUMN dungeon TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hero ADD COLUMN dungeonsCleared INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hero ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pets ADD COLUMN mount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE log ADD COLUMN mp REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE log ADD COLUMN step INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `journal` (`day` INTEGER NOT NULL, `mood` INTEGER NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`day`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `friends` (`uid` TEXT NOT NULL, `code` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`uid`))")
            }
        }
    }
}
