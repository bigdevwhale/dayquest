package app.dayquest.pixel

import app.dayquest.game.Catalog
import app.dayquest.game.Spot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RoomState(
    val look: Look,
    val pet: String = "",
    val petGrown: Boolean = false,
    val placement: Map<Spot, String> = emptyMap(),
    val wallpaper: String = "wp_plain",
    val floor: String = "fl_planks",
    val hour: Float = 12f,
)

/** The hero's room, side view: wallpaper, floor, furniture in fixed spots, warm lights at night. */
class RoomRenderer(val w: Int, val h: Int) {
    val buffer = PixelBuffer(w, h)
    val floorY = (h * 0.6f).toInt()

    /** Where each spot's sprite is anchored: centre x and bottom y. */
    fun anchor(spot: Spot): Pair<Int, Int> = when (spot) {
        Spot.WALL_L -> (w * 0.2f).roundToInt() to (floorY * 0.68f).roundToInt()
        Spot.WALL_C -> (w * 0.5f).roundToInt() to (floorY * 0.62f).roundToInt()
        Spot.WALL_R -> (w * 0.8f).roundToInt() to (floorY * 0.68f).roundToInt()
        Spot.BIG_L -> (w * 0.13f).roundToInt() to floorY + 8
        Spot.BIG_R -> (w * 0.87f).roundToInt() to floorY + 8
        Spot.SMALL_L -> (w * 0.31f).roundToInt() to floorY + 11
        Spot.SMALL_R -> (w * 0.69f).roundToInt() to floorY + 11
        Spot.RUG -> (w * 0.5f).roundToInt() to h - 4
    }

    private fun nightness(hour: Float) = when {
        hour < 5f || hour >= 21f -> 1f
        hour < 7.5f -> 1f - (hour - 5f) / 2.5f
        hour < 18f -> 0f
        else -> (hour - 18f) / 3f
    }.coerceIn(0f, 1f)

    fun render(s: RoomState, now: Long) {
        val b = buffer
        val night = nightness(s.hour)
        drawWall(s.wallpaper, now)
        drawFloor(s.floor)
        // Baseboard
        for (x in 0 until w) {
            b.px[(floorY - 2) * w + x] = 0xFF7A5034.toInt()
            b.px[(floorY - 1) * w + x] = 0xFF5A3A24.toInt()
        }
        val frame = ((now / 450) % 2).toInt()
        val fire = ((now / 220) % 2).toInt()
        val lights = ArrayList<Triple<Int, Int, Int>>()

        // Wall items, the window with the real sky behind its glass.
        for (spot in listOf(Spot.WALL_L, Spot.WALL_C, Spot.WALL_R)) {
            val key = s.placement[spot] ?: continue
            val spr = Sprites.decor(key, frame)
            val (cx, by) = anchor(spot)
            val x = cx - spr.w / 2
            val y = by - spr.h
            if (key == "window") drawSky(x + 3, y + 3, spr.w - 6, spr.h - 6, s.hour, now)
            b.blit(spr, x, y)
        }
        s.placement[Spot.RUG]?.let { key ->
            val spr = Sprites.decor(key)
            val (cx, by) = anchor(Spot.RUG)
            b.blit(spr, cx - spr.w / 2, by - spr.h)
        }
        for (spot in listOf(Spot.BIG_L, Spot.BIG_R, Spot.SMALL_L, Spot.SMALL_R)) {
            val key = s.placement[spot] ?: continue
            val spr = Sprites.decor(key, if (key == "fireplace") fire else frame)
            val (cx, by) = anchor(spot)
            val x = cx - spr.w / 2
            val y = by - spr.h
            b.blit(spr, x, y, flip = spot == Spot.BIG_R && key in setOf("bed", "desk", "piano"))
            when (key) {
                "fireplace" -> lights += Triple(cx, y + spr.h - 6, 34)
                "lamp" -> lights += Triple(cx, y + 3, 26)
                "candles" -> lights += Triple(cx, y + 2, 18)
                "aquarium" -> lights += Triple(cx, y + 6, 14)
            }
        }
        for (spot in listOf(Spot.WALL_L, Spot.WALL_C, Spot.WALL_R)) if (s.placement[spot] == "window" && night < 0.5f) {
            val (cx, by) = anchor(spot)
            lights += Triple(cx, by - 9, 30)
        }

        // Hero and pet in the middle of the room.
        val bob = if ((now / 520) % 2 == 1L) 1 else 0
        val hero = Sprites.hero(s.look, blink = now % 3400 < 140)
        val hx = (w * 0.5f).roundToInt() - hero.w / 2 - 6
        val hy = h - 3 - hero.h + bob
        shadow(hx + hero.w / 2, h - 3, 10)
        b.blit(hero, hx, hy, glowAlpha = (150 + 80 * sin(now / 500.0)).toInt())
        Catalog.petParts(s.pet)?.let { (sp, po) ->
            val pet = Sprites.pet(sp, po, s.petGrown, blink = (now + 900) % 4100 < 140)
            val hopT = now % 2600
            val hop = if (hopT < 300) (sin(hopT / 300.0 * Math.PI) * 3).roundToInt() else 0
            val inBed = s.placement.entries.firstOrNull { it.value == "pet_bed" }?.key
            val (px, py) = if (inBed != null && night > 0.5f) {
                val (cx, by) = anchor(inBed)
                cx - pet.w / 2 to by - pet.h + 1
            } else hx + hero.w - 2 to h - 2 - pet.h - hop
            shadow(px + pet.w / 2, h - 3, 8)
            b.blit(pet, px, py, flip = true)
        }

        if (night > 0f) {
            shade(night)
            for ((lx, ly, r) in lights) warmLight(lx, ly, r, night)
        }
    }

    private fun shadow(cx: Int, y: Int, width: Int) {
        for (x in cx - width / 2..cx + width / 2) buffer.plot(x, y, 0x40000018)
    }

    private fun drawWall(key: String, now: Long) {
        val b = buffer
        for (y in 0 until floorY) for (x in 0 until w) {
            val shadeTop = (1f - y / floorY.toFloat()) * 0.12f
            val c = when (key) {
                "wp_stripes" -> if ((x / 4) % 2 == 0) 0xFFE8C9C0.toInt() else 0xFFF2DCD5.toInt()
                "wp_dots" -> if (x % 6 == 2 && y % 6 == 2 || x % 6 == 5 && y % 6 == 5) 0xFFAFC8DF.toInt() else 0xFFD3E3F0.toInt()
                "wp_bricks" -> {
                    val row = y / 5
                    val off = if (row % 2 == 0) 0 else 5
                    if (y % 5 == 4 || (x + off) % 10 == 0) 0xFFB89A88.toInt() else if ((x + off) / 10 % 3 == 0) 0xFFC2614A.toInt() else 0xFFB5563F.toInt()
                }
                "wp_wood" -> if (x % 7 == 0) 0xFF7A5034.toInt() else if ((x / 7) % 2 == 0) 0xFFA8754A.toInt() else 0xFF9E6C42.toInt()
                "wp_stars" -> {
                    val star = ((x * 73 + y * 151) % 97 == 0)
                    if (star) (if ((now / 600 + x) % 4 == 0L) 0xFFFFF3B0.toInt() else 0xFFF2C14E.toInt())
                    else Bayer.gradient(intArrayOf(0xFF1B1F4A.toInt(), 0xFF2B2350.toInt()), y / floorY.toFloat(), x, y)
                }
                else -> Bayer.gradient(intArrayOf(0xFFE2D3BA.toInt(), 0xFFEDE2CF.toInt()), y / floorY.toFloat(), x, y)
            }
            b.px[y * w + x] = Color.mix(c, 0xFF000000.toInt(), shadeTop)
        }
    }

    private fun drawFloor(key: String) {
        val b = buffer
        for (y in floorY until h) for (x in 0 until w) {
            val ry = y - floorY
            val c = when (key) {
                "fl_tiles" -> if (x % 8 == 0 || ry % 5 == 0) 0xFFB8C4D0.toInt() else 0xFFE4ECF3.toInt()
                "fl_stone" -> {
                    val cell = (x / 7 + (ry / 5) * 3) % 4
                    if (x % 7 == 0 || ry % 5 == 0) 0xFF6E7488.toInt() else intArrayOf(0xFF9AA0B5.toInt(), 0xFF8E94A8.toInt(), 0xFFA6ACC0.toInt(), 0xFF949AAE.toInt())[cell]
                }
                "fl_carpet" -> Bayer.gradient(intArrayOf(0xFF6A3F7A.toInt(), 0xFF5A3368.toInt()), ry / (h - floorY).toFloat(), x, y)
                "fl_checker" -> if ((x / 6 + ry / 4) % 2 == 0) 0xFF2E2433.toInt() else 0xFFE9E3D3.toInt()
                else -> {
                    val row = ry / 4
                    val off = (row * 13) % 17
                    if (ry % 4 == 0 || (x + off) % 17 == 0) 0xFF7A5034.toInt() else if (row % 2 == 0) 0xFFB07A45.toInt() else 0xFFA36E3E.toInt()
                }
            }
            b.px[y * w + x] = c
        }
    }

    private fun drawSky(x0: Int, y0: Int, ww: Int, hh: Int, hour: Float, now: Long) {
        val n = nightness(hour)
        val day = intArrayOf(0xFF4A9BEF.toInt(), 0xFFD4ECFA.toInt())
        val dusk = intArrayOf(0xFF2F3374.toInt(), 0xFFFFAE6B.toInt())
        val dark = intArrayOf(0xFF0B0D24.toInt(), 0xFF1B1F4A.toInt())
        val stops = when {
            n > 0.7f -> dark
            n > 0.1f -> dusk
            else -> day
        }
        for (y in 0 until hh) for (x in 0 until ww) {
            buffer.plot(x0 + x, y0 + y, Bayer.gradient(stops, y / max(1, hh - 1).toFloat(), x, y))
        }
        if (n > 0.7f) {
            buffer.plot(x0 + ww / 3, y0 + 2, 0xFFFFF6E0.toInt()); buffer.plot(x0 + ww - 3, y0 + 4, 0xFFFFF6E0.toInt())
            buffer.rect(x0 + ww - 6, y0 + 1, x0 + ww - 5, y0 + 2, 0xFFF1EEDC.toInt())
        } else if (n <= 0.1f) {
            val cx = x0 + ((now / 900) % (ww + 8)).toInt() - 4
            buffer.rect(cx, y0 + hh / 3, cx + 4, y0 + hh / 3 + 1, 0xE0FFFFFF.toInt())
        }
    }

    private fun shade(night: Float) {
        val k = night * 0.45f
        val tint = 0xFF141A3C.toInt()
        for (i in buffer.px.indices) buffer.px[i] = Color.mix(buffer.px[i], tint, k)
    }

    /** A soft warm pool of light that brightens what it touches. */
    private fun warmLight(cx: Int, cy: Int, r: Int, night: Float) {
        val warm = 0xFFFFC27A.toInt()
        for (y in cy - r..cy + r) for (x in cx - r..cx + r) {
            if (x !in 0 until w || y !in 0 until h) continue
            val d = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toFloat()) / r
            if (d >= 1f) continue
            val a = (1f - d) * (1f - d) * 0.5f * night
            if (a < Bayer.at(x, y) * 0.08f) continue
            val i = y * w + x
            buffer.px[i] = Color.mix(buffer.px[i], Color.mix(buffer.px[i], warm, 0.5f).let { lighten(it) }, a)
        }
    }

    private fun lighten(c: Int) = Color.argb(255, (Color.r(c) * 1.35f).toInt().coerceAtMost(255), (Color.g(c) * 1.3f).toInt().coerceAtMost(255), (Color.b(c) * 1.15f).toInt().coerceAtMost(255))
}
