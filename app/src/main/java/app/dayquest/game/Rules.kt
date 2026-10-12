package app.dayquest.game

import app.dayquest.data.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Difficulty(val mult: Double) { TRIVIAL(0.1), EASY(1.0), MEDIUM(1.5), HARD(2.0) }

data class Stats(val str: Int = 0, val int: Int = 0, val con: Int = 0, val per: Int = 0) {
    operator fun plus(o: Stats) = Stats(str + o.str, int + o.int, con + o.con, per + o.per)
}

/** The numbers of the game. Pure functions, so they are easy to test and to tune. */
object Rules {
    const val MAX_HP = 50.0
    const val HEALTH_POTION_COST = 25.0
    const val HEALTH_POTION_HP = 15.0
    const val FAVORITE_FOOD = 20
    const val OTHER_FOOD = 8

    /** Habitica's curve: 150 XP for level 2, then a little more each level. */
    fun xpToNext(level: Int): Int = (((0.25 * level * level + 10 * level + 139.75) / 10).roundToInt() * 10)

    /** How much a task's value moves: big for red (neglected) tasks, small for blue (mastered) ones. */
    fun delta(value: Double, up: Boolean): Double {
        val v = value.coerceIn(-47.27, 21.27)
        val d = 0.9747.pow(v)
        return if (up) d else -d
    }

    /** Streaks pay a little extra, up to +50%. */
    fun streakBonus(streak: Int) = 1.0 + (streak.coerceAtMost(50) / 100.0)

    fun xpGain(delta: Double, d: Difficulty, s: Stats, bonus: Double = 1.0) =
        delta * d.mult * 10.0 * (1 + s.int / 100.0) * bonus

    fun goldGain(delta: Double, d: Difficulty, s: Stats, bonus: Double = 1.0) =
        delta * d.mult * (1 + s.per / 100.0) * bonus

    /** Constitution softens every hit, never below a third. */
    fun defense(s: Stats) = (1.0 / (1 + s.con / 40.0)).coerceAtLeast(0.33)

    fun damage(delta: Double, d: Difficulty, s: Stats) = kotlin.math.abs(delta) * d.mult.coerceAtLeast(0.5) * 2.0 * defense(s)

    fun bossDamage(delta: Double, d: Difficulty, s: Stats, crit: Boolean) =
        3.0 * delta.coerceAtLeast(0.2) * d.mult * (1 + s.str / 100.0) * if (crit) 1.75 else 1.0

    fun critChance(s: Stats) = (0.05 + s.str * 0.004).coerceAtMost(0.35)

    fun dropChance(d: Difficulty, s: Stats) = (0.12 + 0.08 * d.mult + s.per * 0.002).coerceAtMost(0.5)

    fun dropCap(level: Int) = 5 + level / 3

    /** Habitica's colour bands, from neglected (dark red) to mastered (blue). */
    fun band(value: Double): Int = when {
        value < -20 -> 0
        value < -10 -> 1
        value < -1 -> 2
        value < 1 -> 3
        value < 5 -> 4
        value < 10 -> 5
        else -> 6
    }

    /** The day as the player feels it: before [dayStartHour] it is still yesterday. */
    fun logicalDay(now: LocalDateTime, dayStartHour: Int): Long = now.minusHours(dayStartHour.toLong()).toLocalDate().toEpochDay()

    fun isDue(t: Task, day: Long): Boolean {
        if (day < t.startDay) return false
        if (t.everyDays > 0) return (day - t.startDay) % t.everyDays == 0L
        val dow = LocalDate.ofEpochDay(day).dayOfWeek
        return t.repeatMask and (1 shl (dow.value - 1)) != 0
    }

    fun maskOf(days: Set<DayOfWeek>) = days.fold(0) { m, d -> m or (1 shl (d.value - 1)) }

    fun daysOf(mask: Int) = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}
