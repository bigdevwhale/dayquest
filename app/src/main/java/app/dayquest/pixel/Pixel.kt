package app.dayquest.pixel

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A tiny pixel-art engine in pure Kotlin (no Android types, so sprites render in unit tests too).
 *
 * Sprites are drawn as flat "materials"; [PixelCanvas.render] then shades them the way a pixel
 * artist would: light from the top-left, hue-shifted ramps (cool purple shadows, warm highlights),
 * coloured outlines instead of black ones, contact shadows where one material overlaps another,
 * and an optional glow halo for enchanted things.
 */
class Sprite(val w: Int, val h: Int, val px: IntArray, val glow: IntArray? = null) {
    fun at(x: Int, y: Int) = px[y * w + x]
}

/** How a pixel gets its colour. */
sealed interface Ink

/** A shaded material with a 5-tone ramp: 0 outline, 1 shadow, 2 base, 3 light, 4 highlight. */
class Mat(val ramp: IntArray, val glows: Boolean = false, val outlined: Boolean = true) : Ink

/** A fixed colour (eyes, shines, tiny details). */
class Flat(val color: Int) : Ink

/** One fixed tone of a ramp, for hand-placed shading (stripes, folds, shell plates). */
class Tone(val ramp: IntArray, val i: Int) : Ink

object Color {
    fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    fun a(c: Int) = c ushr 24
    fun r(c: Int) = (c shr 16) and 0xFF
    fun g(c: Int) = (c shr 8) and 0xFF
    fun b(c: Int) = c and 0xFF

    fun mix(c1: Int, c2: Int, t: Float): Int {
        val u = 1 - t
        return argb(
            (a(c1) * u + a(c2) * t).roundToInt(),
            (r(c1) * u + r(c2) * t).roundToInt(),
            (g(c1) * u + g(c2) * t).roundToInt(),
            (b(c1) * u + b(c2) * t).roundToInt(),
        )
    }

    fun withAlpha(c: Int, alpha: Int) = (c and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /** Source-over blend of [src] onto opaque [dst]. */
    fun over(dst: Int, src: Int): Int {
        val sa = a(src)
        if (sa == 255) return src
        if (sa == 0) return dst
        val t = sa / 255f
        return argb(255, (r(dst) * (1 - t) + r(src) * t).roundToInt(), (g(dst) * (1 - t) + g(src) * t).roundToInt(), (b(dst) * (1 - t) + b(src) * t).roundToInt())
    }

    fun luma(c: Int) = (0.299f * r(c) + 0.587f * g(c) + 0.114f * b(c)) / 255f

    fun toHsv(c: Int): FloatArray {
        val r = r(c) / 255f
        val g = g(c) / 255f
        val b = b(c) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0) it + 360f else it }
        return floatArrayOf(h, if (mx == 0f) 0f else d / mx, mx)
    }

    fun fromHsv(h: Float, s: Float, v: Float, alpha: Int = 255): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1 - abs((hh / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) = when ((hh / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(alpha, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    fun darken(c: Int, f: Float) = argb(a(c), (r(c) * f).roundToInt(), (g(c) * f).roundToInt(), (b(c) * f).roundToInt())
}

object Ramps {
    private val cache = HashMap<Int, IntArray>()

    /** Moves hue [h] toward [target] by up to [deg] degrees along the short way round. */
    private fun toward(h: Float, target: Float, deg: Float): Float {
        var diff = ((target - h + 540f) % 360f) - 180f
        if (abs(diff) < deg) return target
        diff = if (diff > 0) deg else -deg
        return (h + diff + 360f) % 360f
    }

    /** A hue-shifted 5-tone ramp around [base]: the classic way to make pixel art look rich instead of muddy. */
    fun of(base: Int): IntArray = synchronized(cache) { cache.getOrPut(base) { build(base) } }

    private fun build(base: Int): IntArray {
        val (h, s, v) = Color.toHsv(base).let { Triple(it[0], it[1], it[2]) }
        val grey = s < 0.08f
        fun tone(step: Float): Int {
            val hh = when {
                grey && step < 0 -> 235f
                grey -> 45f
                step < 0 -> toward(h, 255f, -step * 13f)
                else -> toward(h, 52f, step * 9f)
            }
            val sat = when {
                grey -> (s + abs(step) * if (step < 0) 0.07f else 0.03f)
                step < 0 -> s + (-step) * 0.07f
                else -> s - step * 0.11f
            }.coerceIn(0f, 1f)
            val value = (v + step * if (step < 0) 0.15f else 0.11f).coerceIn(0.06f, 1f)
            return Color.fromHsv(hh, sat, value)
        }
        val outlineHue = if (grey) 245f else toward(h, 262f, 34f)
        val outline = Color.fromHsv(outlineHue, (s * 0.7f + 0.25f).coerceIn(0f, 0.85f), (v * 0.3f).coerceIn(0.07f, 0.28f))
        return intArrayOf(outline, tone(-1.1f), base, tone(0.9f), tone(1.8f))
    }
}

class PixelCanvas(val w: Int, val h: Int) {
    val ink = arrayOfNulls<Ink>(w * h)

    operator fun get(x: Int, y: Int): Ink? = if (x in 0 until w && y in 0 until h) ink[y * w + x] else null

    fun set(x: Int, y: Int, i: Ink?) {
        if (x in 0 until w && y in 0 until h) ink[y * w + x] = i
    }

    /**
     * Stamps an ASCII grid. '.' and ' ' leave the pixel alone, '_' erases it, anything else is looked
     * up in [legend] (a missing char is a bug, so it fails loudly).
     */
    fun grid(rows: List<String>, legend: Map<Char, Ink>, dx: Int = 0, dy: Int = 0, flip: Boolean = false) {
        val width = rows.maxOf { it.length }
        rows.forEachIndexed { y, row ->
            require(row.length == width) { "Ragged sprite row $y: '$row' (${row.length} != $width)" }
            row.forEachIndexed { x, ch ->
                val tx = if (flip) dx + width - 1 - x else dx + x
                when (ch) {
                    '.', ' ' -> Unit
                    '_' -> set(tx, dy + y, null)
                    else -> set(tx, dy + y, legend[ch] ?: error("No ink for '$ch'"))
                }
            }
        }
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, i: Ink?) {
        for (y in y0..y1) for (x in x0..x1) set(x, y, i)
    }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, i: Ink?) {
        for (y in floor(cy - ry).toInt()..(cy + ry).toInt()) for (x in floor(cx - rx).toInt()..(cx + rx).toInt()) {
            val nx = (x + 0.5f - cx) / rx
            val ny = (y + 0.5f - cy) / ry
            if (nx * nx + ny * ny <= 1f) set(x, y, i)
        }
    }

    fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, i: Ink?) {
        val minX = floor(min(ax, min(bx, cx))).toInt()
        val maxX = max(ax, max(bx, cx)).toInt()
        val minY = floor(min(ay, min(by, cy))).toInt()
        val maxY = max(ay, max(by, cy)).toInt()
        fun edge(px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float) = (x1 - x0) * (py - y0) - (y1 - y0) * (px - x0)
        for (y in minY..maxY) for (x in minX..maxX) {
            val px = x + 0.5f
            val py = y + 0.5f
            val e0 = edge(px, py, ax, ay, bx, by)
            val e1 = edge(px, py, bx, by, cx, cy)
            val e2 = edge(px, py, cx, cy, ax, ay)
            if ((e0 >= 0 && e1 >= 0 && e2 >= 0) || (e0 <= 0 && e1 <= 0 && e2 <= 0)) set(x, y, i)
        }
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, i: Ink?, thick: Int = 1) {
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            for (oy in 0 until thick) for (ox in 0 until thick) set(x + ox, y + oy, i)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    /** Recolours every pixel of one ink. */
    fun replace(from: Ink, to: Ink?) {
        for (k in ink.indices) if (ink[k] === from) ink[k] = to
    }

    /**
     * A height field for shading: each pixel's distance to the edge of its own material (chamfer,
     * capped), bent into a dome profile so edges round off quickly and wide areas stay calm.
     */
    private fun domes(cap: Float = 4f): FloatArray {
        val d = FloatArray(w * h)
        val big = 1e6f
        for (k in d.indices) d[k] = if (ink[k] is Mat) big else 0f
        // Painted details (eyes, stripes) sit on the surface: they don't cut a shape into pieces.
        fun same(k: Int, x: Int, y: Int) = x in 0 until w && y in 0 until h && ink[y * w + x].let { it === ink[k] || (it != null && it !is Mat) }
        fun relax(k: Int, x: Int, y: Int, dx: Int, dy: Int, cost: Float) {
            val nx = x + dx
            val ny = y + dy
            val v = if (same(k, nx, ny)) d[ny * w + nx] + cost else cost
            if (v < d[k]) d[k] = v
        }
        for (y in 0 until h) for (x in 0 until w) {
            val k = y * w + x
            if (d[k] == 0f) continue
            relax(k, x, y, -1, 0, 1f); relax(k, x, y, 0, -1, 1f); relax(k, x, y, -1, -1, 1.4f); relax(k, x, y, 1, -1, 1.4f)
        }
        for (y in h - 1 downTo 0) for (x in w - 1 downTo 0) {
            val k = y * w + x
            if (d[k] == 0f) continue
            relax(k, x, y, 1, 0, 1f); relax(k, x, y, 0, 1, 1f); relax(k, x, y, 1, 1, 1.4f); relax(k, x, y, -1, 1, 1.4f)
        }
        for (k in d.indices) if (d[k] > 0f) {
            val t = (d[k].coerceAtMost(cap) / cap)
            d[k] = cap * kotlin.math.sqrt(1f - (1f - t) * (1f - t))
        }
        return d
    }

    /** Shades, outlines and pads by one pixel on every side (room for the outline). */
    fun render(outline: Boolean = true): Sprite {
        val W = w + 2
        val H = h + 2
        val out = IntArray(W * H)
        fun opaque(x: Int, y: Int) = this[x, y] != null
        val height = domes()
        // Tiny sprites (pets, icons) keep crisp hand-drawn faces; big ones (bosses, furniture) get full volume.
        val domeGain = ((min(w, h) - 12) / 14f).coerceIn(0.3f, 1f)
        fun hAt(x: Int, y: Int, m: Ink, fx: Int, fy: Int): Float {
            val n = this[x, y]
            return when {
                n === m -> height[y * w + x]
                n != null && n !is Mat -> height[fy * w + fx]
                else -> 0f
            }
        }
        for (y in 0 until h) for (x in 0 until w) {
            val i = this[x, y] ?: continue
            out[(y + 1) * W + x + 1] = when (i) {
                is Flat -> i.color
                is Tone -> i.ramp[i.i]
                is Mat -> {
                    val upT = !opaque(x, y - 1)
                    val leftT = !opaque(x - 1, y)
                    val downT = !opaque(x, y + 1)
                    val rightT = !opaque(x + 1, y)
                    val above = this[x, y - 1]
                    val occluded = above != null && above !== i && above is Mat && above.outlined
                    var light = 0f
                    if (upT) light += 1f
                    if (leftT) light += 0.5f
                    if (downT) light -= 1f
                    if (rightT) light -= 0.5f
                    // Volume: every part of a material is a soft dome lit from the top-left, so blobs
                    // read as round things instead of flat cut-outs.
                    val gx = hAt(x + 1, y, i, x, y) - hAt(x - 1, y, i, x, y)
                    val gy = hAt(x, y + 1, i, x, y) - hAt(x, y - 1, i, x, y)
                    val nz = 2f
                    val len = kotlin.math.sqrt(gx * gx + gy * gy + nz * nz)
                    val dome = ((-gx * -0.55f + -gy * -0.75f + nz * 0.37f) / len - 0.37f) * 2.6f * domeGain
                    light = light * 0.55f + dome
                    if (occluded) light -= 1f
                    var t = (2 + light).roundToInt()
                    if (t >= 4 && !(upT && leftT) && dome < 0.9f) t = 3
                    i.ramp[t.coerceIn(1, 4)]
                }
            }
        }
        if (outline) {
            for (y in -1..h) for (x in -1..w) {
                if (opaque(x, y)) continue
                // Prefer the material below or to the right: the outline then reads as that shape's edge.
                val below = this[x, y + 1]
                val right = this[x + 1, y]
                val n = listOf(below, right, this[x - 1, y], this[x, y - 1]).firstOrNull {
                    it != null && (it !is Mat || it.outlined)
                } ?: continue
                // Selective outline: where light hits (above and left of a shape), the line softens.
                val lit = (n === below || n === right) && this[x - 1, y] == null && this[x, y - 1] == null
                out[(y + 1) * W + x + 1] = when (n) {
                    is Mat -> if (lit) Color.mix(n.ramp[0], n.ramp[1], 0.45f) else n.ramp[0]
                    is Tone -> n.ramp[0]
                    is Flat -> Color.darken(n.color, 0.35f)
                }
            }
        }
        val glow = if (ink.any { it is Mat && it.glows }) IntArray(W * H).also { g ->
            for (y in 0 until h) for (x in 0 until w) {
                val i = this[x, y] as? Mat ?: continue
                if (!i.glows) continue
                for (oy in -2..2) for (ox in -2..2) {
                    val gx = x + ox + 1
                    val gy = y + oy + 1
                    if (gx !in 0 until W || gy !in 0 until H) continue
                    val d = max(abs(ox), abs(oy))
                    val alpha = if (d <= 1) 0x70 else 0x30
                    val k = gy * W + gx
                    if (Color.a(g[k]) < alpha) g[k] = Color.withAlpha(i.ramp[4], alpha)
                }
            }
        } else null
        return Sprite(W, H, out, glow)
    }
}

/** Composites sprites into a pixel buffer (the scene) with alpha. */
class PixelBuffer(val w: Int, val h: Int) {
    val px = IntArray(w * h)

    fun clear(c: Int) = px.fill(c)

    fun plot(x: Int, y: Int, c: Int) {
        if (x !in 0 until w || y !in 0 until h) return
        val k = y * w + x
        px[k] = Color.over(px[k], c)
    }

    fun blit(s: Sprite, x: Int, y: Int, flip: Boolean = false, tint: Int = 0, alpha: Int = 255, glowAlpha: Int = 0) {
        if (glowAlpha > 0 && s.glow != null) {
            for (sy in 0 until s.h) for (sx in 0 until s.w) {
                val c = s.glow[sy * s.w + (if (flip) s.w - 1 - sx else sx)]
                if (Color.a(c) == 0) continue
                plot(x + sx, y + sy, Color.withAlpha(c, Color.a(c) * glowAlpha / 255))
            }
        }
        for (sy in 0 until s.h) for (sx in 0 until s.w) {
            var c = s.px[sy * s.w + (if (flip) s.w - 1 - sx else sx)]
            if (Color.a(c) == 0) continue
            if (tint != 0) c = Color.mix(c, Color.withAlpha(tint, 255), Color.a(tint) / 255f)
            if (alpha < 255) c = Color.withAlpha(c, Color.a(c) * alpha / 255)
            plot(x + sx, y + sy, c)
        }
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        for (y in max(0, y0)..min(h - 1, y1)) for (x in max(0, x0)..min(w - 1, x1)) plot(x, y, c)
    }
}

/** 4x4 ordered dither: smooth gradients that still look hand-pixelled. */
object Bayer {
    private val m = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)
    fun at(x: Int, y: Int) = (m[(y and 3) * 4 + (x and 3)] + 0.5f) / 16f

    /** Picks between a band's two colours by the dither threshold. */
    fun gradient(stops: IntArray, t: Float, x: Int, y: Int): Int {
        val p = t.coerceIn(0f, 1f) * (stops.size - 1)
        val i = p.toInt().coerceAtMost(stops.size - 2)
        val f = p - i
        return if (f > at(x, y)) stops[i + 1] else stops[i]
    }
}
