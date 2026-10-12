package app.dayquest.game

import androidx.annotation.PluralsRes
import app.dayquest.R

/** What achievements measure. Most of them only grow with real time, not with grinding. */
enum class Metric(@PluralsRes val title: Int, val targets: List<Int>) {
    STREAK(R.plurals.ach_streak, listOf(7, 30, 100, 365, 1000)),
    TOGETHER(R.plurals.ach_together, listOf(30, 100, 365, 730, 1825)),
    ACTIVE_DAYS(R.plurals.ach_active, listOf(30, 100, 365, 1000)),
    TASKS(R.plurals.ach_tasks, listOf(100, 1000, 5000, 10000)),
    LEVEL(R.plurals.ach_level, listOf(10, 25, 50, 75, 100)),
    BOSSES(R.plurals.ach_bosses, listOf(1, 6, 25, 100)),
    WEEKLY(R.plurals.ach_weekly, listOf(4, 26, 52, 150)),
    SEASONS(R.plurals.ach_seasons, listOf(1, 4, 8, 20)),
    MYTHIC(R.plurals.ach_mythic, listOf(1, 10)),
    PETS(R.plurals.ach_pets, listOf(10, 30, 60, 88)),
    GROWN(R.plurals.ach_grown, listOf(1, 10, 30)),
    DECOR(R.plurals.ach_decor, listOf(5, 15, 30)),
}

data class Achievement(val metric: Metric, val index: Int) {
    val target get() = metric.targets[index]
    val key get() = "${metric.name}:$target"

    /** Bronze, silver, gold, crystal, legendary. */
    val tier get() = index.coerceAtMost(4)

    val gold get() = listOf(25.0, 75.0, 150.0, 300.0, 600.0)[tier]
}

/** A snapshot of everything achievements look at. */
data class Facts(
    val streak: Int = 0,
    val together: Int = 0,
    val activeDays: Int = 0,
    val tasks: Int = 0,
    val level: Int = 1,
    val bosses: Int = 0,
    val weekly: Int = 0,
    val seasons: Int = 0,
    val mythic: Int = 0,
    val pets: Int = 0,
    val grown: Int = 0,
    val decor: Int = 0,
) {
    fun value(m: Metric) = when (m) {
        Metric.STREAK -> streak
        Metric.TOGETHER -> together
        Metric.ACTIVE_DAYS -> activeDays
        Metric.TASKS -> tasks
        Metric.LEVEL -> level
        Metric.BOSSES -> bosses
        Metric.WEEKLY -> weekly
        Metric.SEASONS -> seasons
        Metric.MYTHIC -> mythic
        Metric.PETS -> pets
        Metric.GROWN -> grown
        Metric.DECOR -> decor
    }
}

object Achievements {
    val all = Metric.entries.flatMap { m -> m.targets.indices.map { Achievement(m, it) } }

    fun earned(f: Facts) = all.filter { f.value(it.metric) >= it.target }
}
