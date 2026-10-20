package app.dayquest.game

import kotlin.random.Random

/**
 * Weather follows the calendar: every three hours the sky rolls again from a seed, weighted by
 * the season, so everyone sees the same rain and it never needs storing. A rainbow follows rain.
 */
enum class Weather {
    CLEAR, CLOUDY, RAIN, STORM, FOG, SNOW, RAINBOW;

    /** Rain or a storm: umbrellas out, the pet hides under a leaf. */
    val wet get() = this == RAIN || this == STORM

    companion object {
        private const val BLOCK = 3

        /** Weights in the order clear, cloudy, rain, storm, fog, snow. */
        private fun weights(s: Season) = when (s) {
            Season.SPRING -> intArrayOf(45, 20, 20, 3, 12, 0)
            Season.SUMMER -> intArrayOf(60, 15, 12, 8, 5, 0)
            Season.AUTUMN -> intArrayOf(35, 25, 22, 3, 15, 0)
            Season.WINTER -> intArrayOf(40, 25, 0, 0, 10, 25)
        }

        private fun roll(day: Long, block: Int, s: Season): Weather {
            val w = weights(s)
            var n = Random(day * 8 + block + 7919).nextInt(w.sum())
            val kinds = arrayOf(CLEAR, CLOUDY, RAIN, STORM, FOG, SNOW)
            for (i in w.indices) {
                if (n < w[i]) return kinds[i]
                n -= w[i]
            }
            return CLEAR
        }

        /** Some places bend the weather: no sky in a cave, little rain in the desert, snow on the peaks. */
        private fun local(w: Weather, kind: SceneKind): Weather = when (kind) {
            SceneKind.CAVE -> CLEAR
            SceneKind.DESERT -> when (w) { RAIN, FOG, SNOW -> CLEAR; STORM -> CLOUDY; else -> w }
            SceneKind.SNOW -> if (w.wet) SNOW else w
            SceneKind.VOLCANO -> if (w == SNOW) CLOUDY else w
            SceneKind.SKY -> if (w == FOG) CLEAR else w
            else -> w
        }

        private fun base(day: Long, block: Int, s: Season, kind: SceneKind): Weather {
            val b = Math.floorMod(block, 8)
            val d = day + Math.floorDiv(block, 8)
            return local(roll(d, b, s), kind)
        }

        /** The weather at [hour] (0..24) of calendar [day] in a place. */
        fun at(day: Long, hour: Float, season: Season, kind: SceneKind): Weather {
            val block = (hour / BLOCK).toInt().coerceIn(0, 7)
            val now = base(day, block, season, kind)
            val before = base(day, block - 1, season, kind)
            // The first hour after rain stops, by daylight, a rainbow hangs in the sky.
            val fresh = hour - block * BLOCK < 1f
            if (fresh && before.wet && (now == CLEAR || now == CLOUDY) && hour in 7f..18f) return RAINBOW
            return now
        }
    }
}
