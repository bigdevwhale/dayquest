package app.dayquest.data

import android.content.Context
import app.dayquest.AppContainer
import app.dayquest.R
import app.dayquest.game.Difficulty
import java.time.LocalDate

/** `-Pdemo` builds only: a mid-game hero with tasks, pets and a boss fight, for screenshots. */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer, context: Context) {
        val db = c.db.game()
        if (db.hero() != null) return
        val today = c.game.today()
        db.saveHero(
            Hero(
                name = "Mira", level = 21, xp = 310.0, hp = 41.0, gold = 486.4, lastDay = today,
                skin = 1, hairStyle = 3, hairColor = 4,
                weapon = "flame_blade", armor = "chainmail", head = "wizard_hat", shield = "kite_shield",
                pet = "fox:golden", scene = "meadow",
                ownedGear = "wooden_sword,tunic,iron_sword,leather,cap,buckler,iron_helm,flame_blade,chainmail,wizard_hat,kite_shield,knight_plate,pumpkin_hat",
                ownedScenes = "meadow,forest,snow", quest = "frost_wyrm#1", questHp = 540.0, questsDone = "slime_king=3,goblin_chief=2,stone_golem=1,frost_wyrm=1,week:2026:41=1,season:summer:2026=1",
                ownedDecor = "wp_plain,fl_planks,wp_bricks,painting_hills,window,trophies,bookshelf,fireplace,aquarium,plant,pet_bed,rug_royal,lamp",
                home = "WALL_L=painting_hills,WALL_C=window,WALL_R=trophies,BIG_L=bookshelf,BIG_R=fireplace,SMALL_L=plant,SMALL_R=pet_bed,RUG=rug_royal",
                wallpaper = "wp_bricks",
                heroClass = "MAGE", mp = 27.0, mount = "owl:aurora", dungeonsCleared = 2, uid = "demo0000mira0000",
            )
        )
        fun s(id: Int) = context.getString(id)
        var pos = 0
        val habits = listOf(
            Task(type = TaskType.HABIT, title = s(R.string.st_water), up = true, down = false, value = 6.0, countUp = 3, difficulty = Difficulty.TRIVIAL),
            Task(type = TaskType.HABIT, title = s(R.string.st_walk), up = true, down = false, value = 2.5, countUp = 1),
            Task(type = TaskType.HABIT, title = s(R.string.st_scroll), up = false, down = true, value = -4.0, countDown = 1),
            Task(type = TaskType.HABIT, title = s(R.string.st_junk), up = true, down = true, value = 0.4),
        )
        val dailies = listOf(
            Task(type = TaskType.DAILY, title = s(R.string.st_bed), streak = 23, bestStreak = 23, doneDay = today, value = 11.0, difficulty = Difficulty.TRIVIAL),
            Task(type = TaskType.DAILY, title = s(R.string.st_read), streak = 9, bestStreak = 15, value = 4.0, reminder = 21 * 60),
            Task(type = TaskType.DAILY, title = s(R.string.st_exercise), streak = 4, bestStreak = 12, doneDay = today, value = 2.0, difficulty = Difficulty.MEDIUM),
            Task(type = TaskType.DAILY, title = s(R.string.st_meditate), streak = 0, value = -3.0),
            Task(type = TaskType.DAILY, title = s(R.string.st_plan), streak = 2, value = 1.0, repeatMask = 0b0011111),
        )
        val todos = listOf(
            Task(type = TaskType.TODO, title = s(R.string.st_todo_desk), value = -6.0, dueDay = LocalDate.now().plusDays(1).toEpochDay()),
            Task(type = TaskType.TODO, title = s(R.string.st_todo_board), value = 0.0, difficulty = Difficulty.MEDIUM),
        )
        val rewards = listOf(
            Task(type = TaskType.REWARD, title = s(R.string.st_reward_episode), cost = 15.0),
            Task(type = TaskType.REWARD, title = s(R.string.st_reward_treat), cost = 10.0),
        )
        (habits + dailies + todos + rewards).forEach { db.insert(it.copy(position = pos++)) }
        db.tasks().firstOrNull { it.type == TaskType.DAILY && it.title == s(R.string.st_read) }?.let { t ->
            db.insertCheck(CheckItem(taskId = t.id, text = "Chapter 3", done = true, position = 0))
            db.insertCheck(CheckItem(taskId = t.id, text = "Notes", position = 1))
        }
        listOf("fox:golden" to 100, "cat:base" to 40, "owl:base" to 0, "slime:rose" to 60, "turtle:moss" to 20, "bunny:base" to 80, "wolf:frost" to 0, "fox:harvest" to 30, "owl:aurora" to 100, "dragon:ember" to 10, "cat:blossom" to 0, "bunny:tide" to 50, "slime:base" to 100)
            .forEach { (k, g) -> db.savePet(Pet(k, g, mount = k == "owl:aurora" || k == "slime:base")) }
        listOf("egg:fox" to 2, "egg:owl" to 1, "egg:cat" to 1, "potion:golden" to 1, "potion:moss" to 2, "potion:harvest" to 2, "food:berry" to 3, "food:fish" to 1, "food:cake" to 2)
            .forEach { (k, n) -> db.saveItem(Item(k, n)) }
        // Some history for the activity heatmap
        // Each logged task belongs to a real daily or habit, so the per-task statistics have a story to tell.
        val owners = db.tasks().filter { it.type == TaskType.DAILY || it.type == TaskType.HABIT }
        for (d in 1..400) {
            val n = ((d * 7) % 5) + if (d % 6 == 0) 0 else 2
            repeat(n) { k ->
                val t = owners[(d + k * 3) % owners.size]
                val kind = if (t.type == TaskType.HABIT) LogKind.UP else LogKind.DAILY
                db.insertLog(LogEntry(taskId = t.id, day = today - d, kind = kind, xp = 10.0, valueDelta = 0.4))
            }
            if (d % 4 == 0) owners.firstOrNull { it.type == TaskType.DAILY }?.let { t ->
                db.insertLog(LogEntry(taskId = t.id, day = today - d, kind = LogKind.MISSED, hp = -2.0, valueDelta = -1.1))
            }
        }
        // A month of moods that loosely follow how busy each day was.
        for (d in 1..40) db.saveJournal(JournalEntry(today - d, mood = (((d * 7) % 5) + if (d % 6 == 0) 0 else 2).coerceIn(1, 5)))
    }
}
