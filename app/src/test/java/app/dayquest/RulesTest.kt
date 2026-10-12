package app.dayquest

import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Difficulty
import app.dayquest.game.Rules
import app.dayquest.game.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class RulesTest {
    @Test
    fun levelCurveStartsAtHabiticaValues() {
        assertEquals(150, Rules.xpToNext(1))
        assertTrue(Rules.xpToNext(10) > Rules.xpToNext(9))
    }

    @Test
    fun neglectedTasksPayMoreThanMasteredOnes() {
        assertTrue(Rules.delta(-10.0, true) > Rules.delta(0.0, true))
        assertTrue(Rules.delta(0.0, true) > Rules.delta(15.0, true))
        assertEquals(1.0, Rules.delta(0.0, true), 1e-9)
        assertEquals(-1.0, Rules.delta(0.0, false), 1e-9)
    }

    @Test
    fun constitutionSoftensDamageButNeverToNothing() {
        val plain = Rules.damage(-1.0, Difficulty.MEDIUM, Stats())
        val tough = Rules.damage(-1.0, Difficulty.MEDIUM, Stats(con = 40))
        assertTrue(tough < plain)
        assertTrue(Rules.damage(-1.0, Difficulty.MEDIUM, Stats(con = 10_000)) >= plain * 0.33 - 1e-9)
    }

    @Test
    fun dayRollsOverAtTheChosenHour() {
        val lateNight = LocalDateTime.of(2026, 10, 12, 2, 30)
        assertEquals(LocalDate.of(2026, 10, 12).toEpochDay(), Rules.logicalDay(lateNight, 0))
        assertEquals(LocalDate.of(2026, 10, 11).toEpochDay(), Rules.logicalDay(lateNight, 4))
    }

    @Test
    fun weekdayAndEveryNDaysSchedules() {
        val monday = LocalDate.of(2026, 10, 12)
        val weekdays = Task(type = TaskType.DAILY, title = "x", repeatMask = Rules.maskOf(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)))
        assertTrue(Rules.isDue(weekdays, monday.toEpochDay()))
        assertFalse(Rules.isDue(weekdays, monday.plusDays(1).toEpochDay()))
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), Rules.daysOf(weekdays.repeatMask))

        val every3 = Task(type = TaskType.DAILY, title = "x", everyDays = 3, startDay = monday.toEpochDay())
        assertTrue(Rules.isDue(every3, monday.toEpochDay()))
        assertFalse(Rules.isDue(every3, monday.plusDays(1).toEpochDay()))
        assertTrue(Rules.isDue(every3, monday.plusDays(6).toEpochDay()))
        assertFalse(Rules.isDue(every3, monday.minusDays(3).toEpochDay()))
    }

    @Test
    fun colourBandsCoverTheRange() {
        assertEquals(0, Rules.band(-30.0))
        assertEquals(3, Rules.band(0.0))
        assertEquals(6, Rules.band(25.0))
    }
}
