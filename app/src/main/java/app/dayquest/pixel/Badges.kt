package app.dayquest.pixel

import app.dayquest.game.HeroClass
import app.dayquest.game.RoomFoe

/** Small icons for classes, mana, moods, focus, the tavern and the dungeon. Same shading engine as [Sprites]. */
object Badges {
    private val WHITE = Flat(0xFFFFFFFF.toInt())
    private val EYE = Flat(0xFF2A1F38.toInt())
    private fun mat(c: Int, glows: Boolean = false) = Mat(Ramps.of(c), glows)
    private fun tone(c: Int, i: Int) = Tone(Ramps.of(c), i)

    val mana: Sprite get() = Sprites.cached("badge:mana") {
        val c = PixelCanvas(7, 9)
        c.grid(
            listOf(
                "...m...",
                "...m...",
                "..mmm..",
                ".mmmmm.",
                "mmWmmmm",
                "mWmmmmm",
                "mmmmmMm",
                ".mmmMm.",
                "..mmm..",
            ),
            mapOf('m' to mat(0xFF4F9BEF.toInt(), glows = true), 'M' to tone(0xFF4F9BEF.toInt(), 1), 'W' to WHITE),
        )
        c.render()
    }

    val saddle: Sprite get() = Sprites.cached("badge:saddle") {
        val c = PixelCanvas(12, 9)
        c.grid(
            listOf(
                "ll........ll",
                ".llllllllll.",
                ".lLllllllLl.",
                "bbbbbbbbbbbb",
                "bbbbbbbbbbbb",
                "gggggggggggg",
                "...s....s...",
                "...s....s...",
                "..ss....ss..",
            ),
            mapOf('l' to mat(0xFF7A4A2A.toInt()), 'L' to tone(0xFF7A4A2A.toInt(), 3), 'b' to mat(0xFFC8553D.toInt()), 'g' to mat(0xFFF2C14E.toInt()), 's' to mat(0xFFB9C3CF.toInt())),
        )
        c.render()
    }

    val hourglass: Sprite get() = Sprites.cached("badge:hourglass") {
        val c = PixelCanvas(9, 11)
        c.grid(
            listOf(
                "wwwwwwwww",
                ".g.....g.",
                ".gsssssg.",
                ".g.sss.g.",
                ".g..s..g.",
                ".g..s..g.",
                ".g.....g.",
                ".g..s..g.",
                ".g.sss.g.",
                ".gsssssg.",
                "wwwwwwwww",
            ),
            mapOf('w' to mat(0xFF9A6334.toInt()), 'g' to mat(0xFFBFD7EA.toInt()), 's' to mat(0xFFF2C14E.toInt(), glows = true)),
        )
        c.render()
    }

    val mug: Sprite get() = Sprites.cached("badge:mug") {
        val c = PixelCanvas(11, 11)
        c.grid(
            listOf(
                ".fffffff...",
                "fffWffffff.",
                "wwwwwwwww..",
                "wbbbbbbbwhh",
                "wbbbbbbbw.h",
                "wbbbbbbbw.h",
                "wbbbbbbbw.h",
                "wbbbbbbbwhh",
                "wbbbbbbbw..",
                "wwwwwwwww..",
                ".wwwwwww...",
            ),
            mapOf('f' to mat(0xFFFFF5E0.toInt()), 'W' to WHITE, 'w' to mat(0xFF9A6334.toInt()), 'b' to mat(0xFFF2A93B.toInt()), 'h' to mat(0xFF6B4630.toInt())),
        )
        c.render()
    }

    /** One emblem per class, in the class colour. */
    fun emblem(cls: HeroClass): Sprite = Sprites.cached("badge:class:${cls.name}") {
        val c = PixelCanvas(11, 11)
        val main = mat(cls.color, glows = cls == HeroClass.MAGE)
        val wood = mat(0xFF8A5A36.toInt())
        val steel = mat(0xFFC4CED9.toInt())
        when (cls) {
            HeroClass.WARRIOR -> c.grid(
                listOf(
                    ".........ss",
                    "........sss",
                    ".......sss.",
                    "......sss..",
                    ".....sss...",
                    ".m..sss....",
                    ".mmsss.....",
                    "..mms......",
                    ".wmmm......",
                    "ww..m......",
                    "w..........",
                ),
                mapOf('s' to steel, 'm' to main, 'w' to wood),
            )
            HeroClass.MAGE -> c.grid(
                listOf(
                    "......m.m..",
                    ".......m...",
                    "....m.mmm.m",
                    ".......m...",
                    "......mWm..",
                    ".....w.m...",
                    "....w......",
                    "...w.......",
                    "..w........",
                    ".w.........",
                    "w..........",
                ),
                mapOf('m' to main, 'w' to wood, 'W' to WHITE),
            )
            HeroClass.ROGUE -> c.grid(
                listOf(
                    "........s..",
                    ".......ss..",
                    "......ss...",
                    ".....ss....",
                    "....ss.....",
                    "..mms......",
                    "...mm......",
                    "..w.m......",
                    ".w.........",
                    "...........",
                    "...........",
                ),
                mapOf('s' to steel, 'm' to main, 'w' to wood),
            )
            HeroClass.HEALER -> c.grid(
                listOf(
                    "...........",
                    "....mmm....",
                    "....mWm....",
                    "....mmm....",
                    ".mmmmmmmmm.",
                    ".mmmmmmmmm.",
                    ".mmmmmmmmm.",
                    "....mmm....",
                    "....mmm....",
                    "....mmm....",
                    "...........",
                ),
                mapOf('m' to main, 'W' to WHITE),
            )
        }
        c.render()
    }

    /** Mood faces, 1 (hard) to 5 (great): red to green, frown to grin. */
    val moodColors = intArrayOf(0xFFE5574F.toInt(), 0xFFF28A4B.toInt(), 0xFFF2C14E.toInt(), 0xFF9BD45A.toInt(), 0xFF4FC07A.toInt())

    fun mood(m: Int): Sprite = Sprites.cached("badge:mood:$m") {
        val i = (m - 1).coerceIn(0, 4)
        val c = PixelCanvas(11, 11)
        c.ellipse(5.5f, 5.5f, 5.3f, 5.3f, mat(moodColors[i]))
        c.set(3, 4, EYE); c.set(7, 4, EYE)
        if (i == 0) { c.set(2, 3, EYE); c.set(8, 3, EYE) }
        val mouth: List<Pair<Int, Int>> = when (i) {
            0 -> listOf(3 to 8, 4 to 7, 5 to 7, 6 to 7, 7 to 8)
            1 -> listOf(4 to 8, 5 to 7, 6 to 8)
            2 -> listOf(3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7)
            3 -> listOf(3 to 7, 4 to 8, 5 to 8, 6 to 8, 7 to 7)
            else -> listOf(2 to 6, 3 to 7, 4 to 8, 5 to 8, 6 to 8, 7 to 7, 8 to 6)
        }
        mouth.forEach { (x, y) -> c.set(x, y, EYE) }
        if (i == 4) { c.set(4, 7, WHITE); c.set(5, 7, WHITE); c.set(6, 7, WHITE) }
        c.set(3, 2, WHITE)
        c.render()
    }

    /** A dungeon room's guardian, small enough for a row of five. */
    fun foe(f: RoomFoe): Sprite = Sprites.boss(f.shape, f.primary, f.secondary)

    /** A closed door for rooms not reached yet. */
    val door: Sprite get() = Sprites.cached("badge:door") {
        val c = PixelCanvas(12, 14)
        val stone = mat(0xFF6F6890.toInt())
        val wood = mat(0xFF7A4A2A.toInt())
        c.rect(0, 2, 11, 13, stone)
        c.ellipse(6f, 3f, 6f, 3f, stone)
        c.rect(2, 4, 9, 13, wood)
        c.ellipse(6f, 4.5f, 4f, 2.5f, wood)
        c.rect(5, 4, 6, 13, tone(0xFF7A4A2A.toInt(), 1))
        c.set(8, 9, mat(0xFFF2C14E.toInt()))
        c.render()
    }

    /** A little cottage: the way home from the task board. */
    val house: Sprite get() = Sprites.cached("badge:house") {
        val c = PixelCanvas(14, 13)
        c.rect(2, 6, 11, 12, mat(0xFFEDE2CF.toInt()))
        c.tri(-0.5f, 7f, 7f, 0f, 14.5f, 7f, mat(0xFFC8553D.toInt()))
        c.rect(10, 1, 11, 3, mat(0xFF9AA0B5.toInt()))
        c.rect(5, 8, 7, 12, mat(0xFF8A5A36.toInt()))
        c.rect(9, 8, 10, 9, mat(0xFFFFD27A.toInt(), glows = true))
        c.render()
    }
}
