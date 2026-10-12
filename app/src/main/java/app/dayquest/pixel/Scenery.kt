package app.dayquest.pixel

import kotlin.math.abs
import kotlin.math.floor

/**
 * Hand-shaded balls: each one lit from the top-left on its own, drawn back to front, so piles of them
 * read as clumps of leaves, puffs of cloud or heaps of rock instead of one flat blob.
 */
fun PixelCanvas.ball(cx: Float, cy: Float, rx: Float, ry: Float, ramp: IntArray, texture: Boolean = true, top: Int = 4) {
    for (y in floor(cy - ry).toInt()..(cy + ry).toInt()) for (x in floor(cx - rx).toInt()..(cx + rx).toInt()) {
        val nx = (x + 0.5f - cx) / rx
        val ny = (y + 0.5f - cy) / ry
        val r2 = nx * nx + ny * ny
        if (r2 > 1f) continue
        val lit = -nx * 0.6f - ny * 0.8f
        val n = noise(x, y)
        var t = when {
            lit > 0.62f && r2 > 0.15f -> 4
            lit > 0.18f -> 3
            lit < -0.42f -> 1
            else -> 2
        }
        // A ragged border between tones looks like leaves rather than a gradient.
        if (texture && n < 0.18f && t > 1) t -= 1 else if (texture && n > 0.9f && t < 3) t += 1
        set(x, y, Tone(ramp, t.coerceAtMost(top)))
    }
}

/** A cheap stable hash in 0..1 for texture. */
fun noise(x: Int, y: Int): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

object Scenery {
    private fun mat(c: Int, glows: Boolean = false) = Mat(Ramps.of(c), glows)
    private fun cached(key: String, build: () -> Sprite) = Sprites.cached("scn:$key", build)

    /** A broad leafy tree; [variant] changes the crown so a row of them doesn't repeat. */
    fun oak(leaf: Int, variant: Int, blossom: Int = 0): Sprite = cached("oak:$leaf:$variant:$blossom") {
        val c = PixelCanvas(26, 30)
        val bark = Ramps.of(0xFF7A5034.toInt())
        // Trunk with flared roots and two branches into the crown
        c.rect(11, 14, 14, 29, Tone(bark, 2)); c.rect(11, 14, 11, 29, Tone(bark, 3)); c.rect(14, 16, 14, 29, Tone(bark, 1))
        c.set(10, 29, Tone(bark, 2)); c.set(9, 29, Tone(bark, 1)); c.set(15, 29, Tone(bark, 1)); c.set(16, 29, Tone(bark, 1))
        c.line(12, 17, 7, 12, Tone(bark, 2)); c.line(13, 16, 18, 11, Tone(bark, 1))
        c.set(12, 22, Tone(bark, 1)); c.set(13, 25, Tone(bark, 1))
        val ramp = Ramps.of(leaf)
        val blobs = when (variant % 3) {
            0 -> listOf(Triple(7f, 13f, 5f), Triple(19f, 13f, 5f), Triple(13f, 11f, 6f), Triple(8f, 7f, 5f), Triple(17f, 6f, 5.5f), Triple(12.5f, 4.5f, 4.5f))
            1 -> listOf(Triple(6f, 12f, 4.5f), Triple(20f, 12f, 4.5f), Triple(13f, 13f, 5f), Triple(9f, 7f, 5.5f), Triple(17f, 8f, 5f), Triple(13f, 3.5f, 4f))
            else -> listOf(Triple(13f, 14f, 6f), Triple(7f, 11f, 4.5f), Triple(19f, 10f, 5f), Triple(11f, 6f, 5.5f), Triple(16f, 4f, 4f))
        }
        blobs.forEach { (x, y, r) -> c.ball(x, y, r, r * 0.9f, ramp) }
        if (blossom != 0) {
            val b = Ramps.of(blossom)
            for (y in 0 until 20) for (x in 0 until 26) if (c[x, y] is Tone && noise(x * 3, y * 5) > 0.8f) c.set(x, y, Tone(b, if (noise(x, y) > 0.5f) 3 else 2))
        }
        c.render()
    }

    /** A layered pine; snow sits on the upper side of each tier. */
    fun pine(leaf: Int, snow: Boolean, tall: Int = 0): Sprite = cached("pine:$leaf:$snow:$tall") {
        val H = 28 + tall * 4
        val c = PixelCanvas(17, H)
        val bark = Ramps.of(0xFF6B4630.toInt())
        c.rect(7, H - 5, 9, H - 1, Tone(bark, 2)); c.rect(9, H - 5, 9, H - 1, Tone(bark, 1))
        val ramp = Ramps.of(leaf)
        val tiers = 4 + tall
        for (k in 0 until tiers) {
            val top = k * (H - 6) / (tiers + 1).toFloat()
            val bottom = top + (H - 6) / (tiers + 1f) * 2.2f
            val half = 2.5f + k * (6f / tiers) + 1f
            for (y in top.toInt()..bottom.toInt()) {
                val f = (y - top) / (bottom - top)
                val hw = half * f
                for (x in (8.5f - hw).toInt()..(8.5f + hw).toInt()) {
                    val side = (x - 8.5f) / (hw + 0.01f)
                    // Light on the left face, shadow under each tier's skirt.
                    val t = when {
                        f > 0.82f -> 1
                        side < -0.35f -> 3
                        side > 0.45f -> 1
                        else -> 2
                    }
                    val tt = if (noise(x, y) > 0.86f && t > 1) t - 1 else t
                    c.set(x, y, Tone(ramp, tt))
                    // Snow lies along the sloping upper edge of each tier.
                    val edgeY = top + kotlin.math.abs(x - 8.5f) / half * (bottom - top)
                    if (snow && y - edgeY < 1.6f && f < 0.85f && noise(x + 7, k) > 0.15f) {
                        c.set(x, y, Tone(Ramps.of(0xFFF4F8FF.toInt()), if (side < 0f) 3 else 2))
                    }
                }
            }
        }
        if (snow) c.set(8, 0, Tone(Ramps.of(0xFFF4F8FF.toInt()), 4))
        c.render()
    }

    fun bush(leaf: Int, berry: Int = 0): Sprite = cached("bush:$leaf:$berry") {
        val c = PixelCanvas(16, 9)
        val ramp = Ramps.of(leaf)
        c.ball(4f, 6f, 4f, 3.4f, ramp); c.ball(11.5f, 6f, 4.4f, 3.4f, ramp); c.ball(8f, 4.5f, 4.5f, 4f, ramp)
        c.rect(0, 8, 15, 8, null)
        if (berry != 0) listOf(3 to 5, 8 to 3, 12 to 5, 10 to 6, 6 to 6).forEach { (x, y) -> c.set(x, y, Flat(berry)); c.set(x, y - 1, Flat(Color.mix(berry, 0xFFFFFFFF.toInt(), 0.5f))) }
        c.render()
    }

    fun rock(color: Int, moss: Boolean = false, big: Boolean = false): Sprite = cached("rock:$color:$moss:$big") {
        val c = if (big) PixelCanvas(18, 11) else PixelCanvas(11, 7)
        val r = Ramps.of(color)
        if (big) {
            c.ball(6f, 7f, 5.5f, 4.5f, r, texture = false); c.ball(12f, 7.5f, 5.5f, 4f, r, texture = false); c.ball(8.5f, 4.5f, 5f, 4.5f, r, texture = false)
            c.rect(1, 10, 16, 10, Tone(r, 1))
        } else {
            c.ball(5.5f, 4.5f, 5f, 3.8f, r, texture = false)
            c.rect(1, 6, 9, 6, Tone(r, 1))
        }
        if (moss) {
            val m = Ramps.of(0xFF6DBA5A.toInt())
            for (x in 0 until c.w) for (y in 0 until c.h) if (c[x, y] != null && c[x, y - 1] == null && noise(x, y) > 0.3f) {
                c.set(x, y, Tone(m, 3)); if (noise(x, y + 9) > 0.5f) c.set(x, y + 1, Tone(m, 2))
            }
        }
        c.render()
    }

    fun flower(petal: Int, tall: Boolean): Sprite = cached("flower:$petal:$tall") {
        val c = PixelCanvas(5, if (tall) 8 else 6)
        val stem = Tone(Ramps.of(0xFF4E9A4A.toInt()), 2)
        val p = Ramps.of(petal)
        c.rect(2, 2, 2, c.h - 1, stem)
        c.set(1, c.h - 2, Tone(Ramps.of(0xFF4E9A4A.toInt()), 3))
        c.set(1, 1, Tone(p, 3)); c.set(3, 1, Tone(p, 2)); c.set(2, 0, Tone(p, 3)); c.set(2, 2, Tone(p, 1))
        c.set(2, 1, Flat(0xFFFFE066.toInt()))
        c.render(outline = false)
    }

    fun mushroom(cap: Int): Sprite = cached("mush:$cap") {
        val c = PixelCanvas(9, 8)
        val r = Ramps.of(cap)
        c.rect(3, 4, 5, 7, Tone(Ramps.of(0xFFF1E6D3.toInt()), 2)); c.set(3, 4, Tone(Ramps.of(0xFFF1E6D3.toInt()), 3))
        c.ball(4.5f, 3.5f, 4.5f, 3f, r, texture = false)
        c.rect(0, 4, 8, 4, null); c.rect(1, 4, 7, 4, Tone(r, 1)); c.rect(3, 4, 5, 4, Tone(Ramps.of(0xFFF1E6D3.toInt()), 1))
        c.set(2, 1, Flat(0xFFFFFFFF.toInt())); c.set(5, 2, Flat(0xFFFFFFFF.toInt())); c.set(6, 1, Flat(0xFFF6EBDD.toInt()))
        c.render()
    }

    fun fence(color: Int = 0xFFB07A45.toInt()): Sprite = cached("fence:$color") {
        val c = PixelCanvas(24, 10)
        val r = Ramps.of(color)
        for (x in listOf(1, 9, 17)) {
            c.rect(x, 1, x + 2, 9, Tone(r, 2)); c.set(x, 1, Tone(r, 3)); c.rect(x + 2, 2, x + 2, 9, Tone(r, 1)); c.set(x + 1, 0, Tone(r, 3))
        }
        c.rect(0, 3, 23, 4, Tone(r, 3)); c.rect(0, 4, 23, 4, Tone(r, 2))
        c.rect(0, 7, 23, 7, Tone(r, 2)); c.rect(0, 8, 23, 8, Tone(r, 1))
        c.render()
    }

    fun log(): Sprite = cached("log") {
        val c = PixelCanvas(16, 6)
        val r = Ramps.of(0xFF8A5A36.toInt())
        c.rect(2, 0, 15, 5, Tone(r, 2)); c.rect(2, 0, 15, 0, Tone(r, 3)); c.rect(2, 5, 15, 5, Tone(r, 1))
        c.ellipse(2f, 3f, 2.2f, 3f, Tone(Ramps.of(0xFFE0B888.toInt()), 2)); c.set(2, 3, Tone(Ramps.of(0xFFE0B888.toInt()), 1))
        listOf(6, 10, 13).forEach { x -> c.set(x, 2, Tone(r, 1)); c.set(x + 1, 2, Tone(r, 1)) }
        c.set(9, 0, Tone(Ramps.of(0xFF6DBA5A.toInt()), 3)); c.set(10, 0, Tone(Ramps.of(0xFF6DBA5A.toInt()), 2))
        c.render()
    }

    fun palm(): Sprite = cached("palm2") {
        val c = PixelCanvas(24, 30)
        val trunk = Ramps.of(0xFFB08452.toInt())
        // A curved, ringed trunk
        for (k in 0..20) {
            val y = 29 - k
            val x = 11 + (k * k) / 110
            c.rect(x, y, x + 2, y, Tone(trunk, if (k % 3 == 0) 1 else 2)); c.set(x, y, Tone(trunk, 3))
        }
        val leaf = Ramps.of(0xFF4FAE5A.toInt())
        fun frond(dx: Int, dy: Int, len: Int) {
            for (k in 0..len) {
                val x = 15 + dx * k / len
                val droop = (k * k) / (len * 1.6f)
                val y = 7 + (dy * k / len) + droop.toInt()
                c.set(x, y, Tone(leaf, 2)); c.set(x, y - 1, Tone(leaf, 3))
                if (k % 2 == 0) c.set(x, y + 1, Tone(leaf, 1))
            }
        }
        frond(-14, -2, 14); frond(8, -1, 9); frond(-9, -6, 10); frond(6, -6, 8); frond(-4, -7, 7); frond(-13, 3, 12)
        c.ball(15f, 8f, 1.8f, 1.6f, Ramps.of(0xFF7A5034.toInt()), texture = false); c.ball(13f, 9f, 1.4f, 1.4f, Ramps.of(0xFF7A5034.toInt()), texture = false)
        c.render()
    }

    fun cactus(): Sprite = cached("cactus2") {
        val c = PixelCanvas(13, 20)
        val g = Ramps.of(0xFF4E9F5A.toInt())
        fun col(x0: Int, x1: Int, y0: Int, y1: Int) {
            for (y in y0..y1) for (x in x0..x1) c.set(x, y, Tone(g, if (x == x0) 3 else if (x == x1) 1 else 2))
            c.set(x0, y0, null); c.set(x1, y0, null)
        }
        col(5, 8, 1, 19); col(1, 3, 6, 12); c.rect(1, 12, 5, 13, Tone(g, 2)); c.rect(1, 13, 5, 13, Tone(g, 1))
        col(10, 12, 4, 10); c.rect(8, 10, 12, 11, Tone(g, 2)); c.rect(8, 11, 12, 11, Tone(g, 1))
        for (y in 3..18 step 3) { c.set(6, y, Tone(g, 4)); c.set(7, y + 1, Tone(g, 3)) }
        c.set(6, 0, Flat(0xFFF28DB2.toInt())); c.set(7, 0, Flat(0xFFFFC6DD.toInt()))
        c.render()
    }

    fun crystal(color: Int): Sprite = cached("crystal2:$color") {
        val c = PixelCanvas(14, 17)
        val r = Ramps.of(color)
        fun shard(x0: Float, top: Float, half: Float, bottom: Float) {
            for (y in top.toInt()..bottom.toInt()) {
                val f = ((y - top) / 3f).coerceAtMost(1f)
                val hw = half * f
                for (x in (x0 - hw).toInt()..(x0 + hw).toInt()) c.set(x, y, Tone(r, if (x < x0 - 0.3f) 3 else if (x > x0 + hw * 0.4f) 1 else 2))
                c.set(x0.toInt(), y, Tone(r, if (y < top + 3) 4 else 3))
            }
        }
        shard(3f, 6f, 2f, 16f); shard(11f, 7f, 2f, 16f); shard(7f, 0f, 2.6f, 16f)
        c.render()
    }

    /** A soft cumulus: shaded puffs, no outline. */
    fun cloud(variant: Int): Sprite = cached("cloud2:$variant") {
        val c = PixelCanvas(34, 13)
        val r = Ramps.of(0xFFF7F4FF.toInt())
        val puffs = when (variant % 3) {
            0 -> listOf(Triple(7f, 9f, 5f), Triple(14f, 6f, 6.5f), Triple(22f, 7.5f, 5.5f), Triple(28f, 9.5f, 4f))
            1 -> listOf(Triple(6f, 9.5f, 4f), Triple(12f, 7f, 5f), Triple(19f, 8.5f, 4.5f))
            else -> listOf(Triple(8f, 9f, 5.5f), Triple(16f, 5.5f, 5.5f), Triple(23f, 8f, 5f), Triple(13f, 9.5f, 4f))
        }
        puffs.forEach { (x, y, rr) -> c.ball(x, y, rr, rr * 0.85f, r, texture = false, top = 3) }
        // Flat underside, slightly shaded
        for (x in 0 until c.w) for (y in 10..12) if (c[x, y] != null) c.set(x, y, if (y == 12) null else Tone(r, 1))
        c.render(outline = false)
    }

    fun deadTree(): Sprite = cached("deadtree") {
        val c = PixelCanvas(18, 24)
        val r = Ramps.of(0xFF3A2C34.toInt())
        c.rect(8, 8, 10, 23, Tone(r, 2)); c.rect(8, 8, 8, 23, Tone(r, 3))
        c.line(9, 14, 3, 8, Tone(r, 2), 1); c.line(3, 8, 2, 4, Tone(r, 2)); c.line(9, 11, 15, 5, Tone(r, 2)); c.line(15, 5, 16, 2, Tone(r, 2))
        c.line(9, 8, 9, 1, Tone(r, 2)); c.line(5, 10, 6, 6, Tone(r, 2)); c.set(7, 23, Tone(r, 1)); c.set(11, 23, Tone(r, 1))
        c.render()
    }

    fun stoneLantern(lit: Boolean): Sprite = cached("lantern:$lit") {
        val c = PixelCanvas(9, 16)
        val s = Ramps.of(0xFFA9AFC2.toInt())
        c.rect(0, 3, 8, 4, Tone(s, 3)); c.rect(1, 2, 7, 2, Tone(s, 3)); c.rect(3, 1, 5, 1, Tone(s, 3)); c.set(4, 0, Tone(s, 3))
        c.rect(1, 5, 7, 9, Tone(s, 2)); c.rect(7, 5, 7, 9, Tone(s, 1))
        c.rect(3, 6, 5, 8, if (lit) Mat(Ramps.of(0xFFFFD27A.toInt()), glows = true) else Flat(0xFF2A2440.toInt()))
        c.rect(2, 10, 6, 10, Tone(s, 3)); c.rect(3, 11, 5, 14, Tone(s, 2)); c.rect(5, 11, 5, 14, Tone(s, 1)); c.rect(1, 15, 7, 15, Tone(s, 1))
        c.render()
    }

    /** Distant castle on its hill: towers, crenellations, banners and lit windows. */
    fun castle(stone: Int, banner: Int, windows: Boolean): Sprite = cached("castle:$stone:$banner:$windows") {
        val c = PixelCanvas(44, 30)
        val s = Ramps.of(stone)
        fun tower(x0: Int, x1: Int, top: Int, roof: Boolean) {
            for (y in top..29) for (x in x0..x1) c.set(x, y, Tone(s, if (x == x0) 3 else if (x >= x1 - 1) 1 else 2))
            if (roof) {
                val r = Ramps.of(0xFF5A4E8C.toInt())
                val mid = (x0 + x1) / 2f
                c.tri(x0 - 1f, top.toFloat(), x1 + 2f, top.toFloat(), mid + 0.5f, top - 8f, Tone(r, 2))
                c.tri(x0 - 1f, top.toFloat(), mid + 0.5f, top.toFloat(), mid + 0.5f, top - 8f, Tone(r, 3))
                c.line(mid.toInt(), top - 9, mid.toInt(), top - 12, Tone(Ramps.of(0xFF5A4636.toInt()), 2))
                c.rect(mid.toInt() + 1, top - 12, mid.toInt() + 3, top - 11, Tone(Ramps.of(banner), 2))
            } else {
                for (x in x0..x1 step 2) c.set(x, top - 1, Tone(s, 3))
            }
        }
        tower(10, 33, 14, false)
        tower(3, 9, 9, true); tower(34, 40, 9, true); tower(18, 25, 6, true)
        c.rect(19, 22, 24, 29, Flat(0xFF2A2236.toInt())); c.ellipse(21.5f, 22f, 2.8f, 2.4f, Flat(0xFF2A2236.toInt()))
        val win = if (windows) Flat(0xFFFFD27A.toInt()) else Flat(0xFF2A2236.toInt())
        listOf(5 to 14, 36 to 14, 21 to 11, 14 to 18, 29 to 18, 6 to 20, 37 to 20).forEach { (x, y) -> c.rect(x, y, x + 1, y + 1, win) }
        c.render()
    }

    /** A hanging banner on a pole for the castle ruins. */
    fun banner(color: Int, frame: Int): Sprite = cached("banner:$color:$frame") {
        val c = PixelCanvas(10, 20)
        val pole = Ramps.of(0xFF5A4636.toInt())
        c.rect(1, 0, 1, 19, Tone(pole, 2)); c.set(1, 0, Tone(Ramps.of(0xFFF2C14E.toInt()), 3))
        val r = Ramps.of(color)
        for (y in 2..12) {
            val wave = if ((y / 3 + frame) % 2 == 0) 0 else 1
            for (x in 2..7 + wave) c.set(x, y, Tone(r, if (x == 2) 3 else if (x >= 6 + wave) 1 else 2))
        }
        c.tri(2f, 12f, 8f, 12f, 5f, 15f, Tone(r, 2))
        c.set(4, 6, Tone(Ramps.of(0xFFF2C14E.toInt()), 3)); c.set(5, 6, Tone(Ramps.of(0xFFF2C14E.toInt()), 3)); c.set(4, 7, Tone(Ramps.of(0xFFF2C14E.toInt()), 2)); c.set(5, 5, Tone(Ramps.of(0xFFF2C14E.toInt()), 4))
        c.render()
    }

    fun ruinPillar(stone: Int, broken: Boolean): Sprite = cached("pillar:$stone:$broken") {
        val c = PixelCanvas(10, 24)
        val s = Ramps.of(stone)
        val top = if (broken) 8 else 2
        c.rect(0, top, 9, top + 1, Tone(s, 3))
        for (y in top + 2..21) for (x in 1..8) c.set(x, y, Tone(s, when (x) { 1, 2 -> 3; 7, 8 -> 1; else -> 2 }))
        if (broken) { c.set(9, 8, null); c.set(8, 8, null); c.set(0, 8, null) }
        c.rect(0, 22, 9, 23, Tone(s, 2)); c.rect(0, 23, 9, 23, Tone(s, 1))
        listOf(3 to 12, 5 to 17).forEach { (x, y) -> c.set(x, y, Tone(s, 1)) }
        val ivy = Ramps.of(0xFF4FAE5A.toInt())
        for (y in top + 4..20) if (noise(1, y) > 0.35f) c.set(1 + (y % 2), y, Tone(ivy, if (y % 3 == 0) 3 else 2))
        c.render()
    }

    /** The rocky underside of a floating island, hanging below the grass line. */
    fun islandRock(width: Int, depth: Int): Sprite = cached("isle:$width:$depth") {
        val c = PixelCanvas(width, depth)
        val r = Ramps.of(0xFF8A6A58.toInt())
        for (x in 0 until width) {
            val f = abs(x - width / 2f) / (width / 2f)
            val d = ((1f - f * f) * depth * (0.75f + 0.25f * noise(x / 3, 1))).toInt().coerceAtLeast(1)
            for (y in 0 until d) c.set(x, y, Tone(r, if (y > d - 3) 1 else if (x < width / 3) 3 else 2))
        }
        for (k in 0 until width / 3) {
            val x = (noise(k, 5) * width).toInt()
            val y = (noise(k, 7) * depth * 0.6f).toInt()
            if (c[x, y] != null) c.set(x, y, Tone(r, 1))
        }
        c.render()
    }
}

/** Shared helper: hash-based value noise in 0..1, smooth along x (for wavy ridges). */
fun smoothNoise(x: Float, seed: Int): Float {
    val i = floor(x).toInt()
    val f = x - i
    val a = noise(i, seed)
    val b = noise(i + 1, seed)
    val t = f * f * (3 - 2 * f)
    return a + (b - a) * t
}
