package app.dayquest.game

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/** One weekend's walk through the dungeon: [room] is the room being fought (== rooms count when cleared). */
data class DungeonRun(val year: Int, val week: Int, val room: Int = 0, val steps: Int = 0) {
    val finished get() = room >= Dungeon.ROOMS.size
}

/** A foe guarding one room: drawn like a boss, but nothing to fight on its own. */
data class RoomFoe(val shape: BossShape, val primary: Int, val secondary: Int)

/**
 * The weekend dungeon: every Saturday and Sunday a new one opens, five rooms deep. Every finished
 * task is a step forward (hard ones two); each cleared room pays, the last one holds a chest and a saddle.
 */
object Dungeon {
    /** Steps each room takes: about two ordinary weekend days for all five. */
    val ROOMS = intArrayOf(2, 3, 4, 4, 5)

    val last get() = ROOMS.size - 1

    fun isOpen(day: Long): Boolean = LocalDate.ofEpochDay(day).dayOfWeek.let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY }

    /** Days until the next Saturday (0 on the weekend). */
    fun daysUntilOpen(day: Long): Int = if (isOpen(day)) 0 else DayOfWeek.SATURDAY.value - LocalDate.ofEpochDay(day).dayOfWeek.value

    fun steps(d: Difficulty) = if (d == Difficulty.HARD) 2 else 1

    fun parse(s: String): DungeonRun? {
        val p = s.split(':').mapNotNull { it.toIntOrNull() }
        return if (p.size == 4) DungeonRun(p[0], p[1], p[2], p[3]) else null
    }

    fun encode(r: DungeonRun) = "${r.year}:${r.week}:${r.room}:${r.steps}"

    /** This week's run: the saved one if it is this week's, otherwise a fresh one. */
    fun current(saved: String, day: Long): DungeonRun {
        val (y, w) = Catalog.weekOf(LocalDate.ofEpochDay(day))
        return parse(saved)?.takeIf { it.year == y && it.week == w } ?: DungeonRun(y, w)
    }

    /** Walks [n] steps on; returns the new run and the rooms cleared on the way. */
    fun advance(r: DungeonRun, n: Int): Pair<DungeonRun, List<Int>> {
        var room = r.room
        var steps = r.steps + n
        val cleared = ArrayList<Int>()
        while (room < ROOMS.size && steps >= ROOMS[room]) {
            steps -= ROOMS[room]
            cleared += room
            room++
        }
        return r.copy(room = room, steps = if (room >= ROOMS.size) 0 else steps) to cleared
    }

    /** Gold a cleared room pays; the last one pays the most. */
    fun roomGold(level: Int, room: Int) = if (room == last) 40.0 + level * 2 else 10.0 + level + room * 4

    private val colors = listOf(
        0xFF6E5E96 to 0xFFE2435F, 0xFF5CCB6F to 0xFF2E2433, 0xFF8E8A99 to 0xFF5EC8FF, 0xFFB45CFF to 0xFF7CFFB2,
        0xFF4A4A5E to 0xFFF2C14E, 0xFFDDE8F5 to 0xFF7CFFB2, 0xFFA0643A to 0xFFF2C14E, 0xFF3A3F5E to 0xFFFF7A3D,
    )

    /** The week's five foes, built from a seed: the same for everyone, new every week. */
    fun foes(year: Int, week: Int): List<RoomFoe> {
        val r = Random(year * 1000 + week * 7 + 3)
        val shapes = BossShape.entries.shuffled(r).take(ROOMS.size)
        return shapes.map { s -> colors[r.nextInt(colors.size)].let { (p, q) -> RoomFoe(s, p.toInt(), q.toInt()) } }
    }
}
