package app.dayquest

import app.dayquest.data.Hero
import app.dayquest.game.Achievements
import app.dayquest.game.Catalog
import app.dayquest.game.Facts
import app.dayquest.game.Forge
import app.dayquest.game.Season
import app.dayquest.game.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LongGameTest {
    @Test
    fun seasonsFollowTheCalendarAndHemisphere() {
        assertEquals(Season.AUTUMN, Season.of(LocalDate.of(2026, 10, 12), southern = false))
        assertEquals(Season.SPRING, Season.of(LocalDate.of(2026, 10, 12), southern = true))
        // One winter spans December to February.
        assertEquals(Season.year(LocalDate.of(2026, 12, 20), false), Season.year(LocalDate.of(2027, 2, 10), false))
    }

    @Test
    fun oldSavesCountBareBossKeysAsOneWin() {
        val h = Hero(questsDone = "slime_king,goblin_chief=3")
        assertEquals(1, h.wins("slime_king"))
        assertEquals(3, h.wins("goblin_chief"))
        assertEquals("slime_king=2", h.withWin("slime_king").split(',').first { it.startsWith("slime") })
    }

    @Test
    fun bossKeysRoundTrip() {
        val elite = Catalog.nextFight(Catalog.bosses[0], 1)
        assertEquals(elite, Catalog.boss(elite.key))
        assertEquals("slime_king", elite.record)
        val weekly = Catalog.weekly(2026, 42, 15)
        assertEquals(weekly, Catalog.boss(weekly.key))
        assertEquals("week:2026:42", weekly.record)
        val seasonal = Catalog.seasonalBoss(Season.WINTER, 2026, 15)
        assertEquals(seasonal, Catalog.boss(seasonal.key))
        assertEquals("season:winter:2026", seasonal.record)
        Season.entries.forEach { s ->
            val all = Catalog.seasonalBosses(s, 2026, 15)
            assertEquals(Catalog.SEASONAL_PER_SEASON, all.size)
            all.forEach { assertEquals(it, Catalog.boss(it.key)) }
            assertEquals(all.size, all.map { it.record }.distinct().size)
        }
        // Rewards never list the same item twice.
        Catalog.bosses.forEach { b -> (1..5).forEach { r -> val k = Catalog.ranked(b, r).rewards.map { it.first }; assertEquals(k.distinct(), k) } }
    }

    @Test
    fun forgeIsEndlessDeterministicAndPricier() {
        assertEquals(0, Forge.tierFor(19))
        assertEquals(1, Forge.tierFor(20))
        assertEquals(17, Forge.tierFor(100))
        (1..30).forEach { t ->
            Slot.entries.forEach { s ->
                val g = Forge.item(t, s)
                assertEquals(g, Catalog.gear(g.key))
                assertTrue(Forge.item(t + 1, s).price > g.price)
                assertTrue(Forge.item(t + 1, s).stats.let { it.str + it.int + it.con + it.per } > g.stats.let { it.str + it.int + it.con + it.per })
            }
        }
    }

    @Test
    fun achievementsUnlockByThresholds() {
        val earned = Achievements.earned(Facts(streak = 30, level = 10, together = 400))
        assertTrue(earned.any { it.key == "STREAK:30" })
        assertTrue(earned.none { it.key == "STREAK:100" })
        assertNotNull(earned.firstOrNull { it.key == "TOGETHER:365" })
    }
}
