package app.dayquest.pixel

import app.dayquest.game.Boss
import app.dayquest.game.Catalog
import app.dayquest.game.SceneKind
import app.dayquest.game.Season
import app.dayquest.game.Species
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Everything the scene shows; the renderer animates it. */
data class SceneState(
    val kind: SceneKind = SceneKind.MEADOW,
    /** Local hour, 0..24 with fractions: drives the sky. */
    val hour: Float = 12f,
    val look: Look? = null,
    val pet: String = "",
    val petGrown: Boolean = false,
    val boss: Boss? = null,
    val resting: Boolean = false,
    /** Real-calendar season: blossom, summer, falling leaves or snow. */
    val season: Season? = null,
    /** The hero's garden instead of a landscape: its placed pieces, by spot. */
    val garden: Map<app.dayquest.game.Spot, String>? = null,
    /** The saddled pet the hero rides: a big beast standing behind them. */
    val mount: String = "",
)

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val max: Float, val color: Int, val gravity: Float = 0f, val size: Int = 1, val glow: Boolean = false,
)

/** Kinds of moments the scene reacts to. */
enum class Burst { GOLD, XP, HIT, CRIT, HURT, LEVEL, DROP, HEAL, VICTORY, FIREWORKS }

/** How far above the mount's highest pixel the rider's sprite starts: head and shoulders show, the rest hides behind. */
private const val RIDER_LIFT = 14

/** The first row with any pixel in it: low beasts (the turtle) seat their rider lower. */
private fun topRow(s: Sprite): Int = (0 until s.h).firstOrNull { y -> (0 until s.w).any { x -> Color.a(s.at(x, y)) > 0 } } ?: 0

/** How far the rider's feet sit above the mount's bottom edge, in sprite pixels (for drawing a rider outside the scene). */
fun riderRise(mount: Sprite, rider: Sprite): Int = mount.h - (topRow(mount) - RIDER_LIFT + rider.h)

class SceneRenderer(val w: Int, val h: Int, ground: Int = 14) {
    val buffer = PixelBuffer(w, h)
    private val particles = ArrayList<Particle>()
    private val ambient = ArrayList<Particle>()
    private val rnd = Random(7)
    private var hurtUntil = 0L
    private var attackUntil = 0L
    private var bossFlashUntil = 0L
    private var levelUntil = 0L
    private var lastT = 0L

    val groundY = h - ground

    /** Where feet touch the ground: the horizon line, or the garden path. */
    var standY = groundY
        private set

    // Actor anchors (feet), recomputed per state.
    private var heroX = 0
    private var bossX = 0

    fun heroCenter() = heroX + Sprites.HERO_W / 2f + 1

    fun burst(kind: Burst, now: Long) {
        val hx = heroX + Sprites.HERO_W / 2f + 1
        val hy = standY - 14f
        when (kind) {
            Burst.GOLD -> repeat(10) {
                particles += Particle(hx + rnd.nextFloat() * 6 - 3, hy, rnd.nextFloat() * 1.6f - 0.8f, -1.6f - rnd.nextFloat() * 1.2f, 0f, 0.9f, if (it % 3 == 0) 0xFFFFF3B0.toInt() else 0xFFF2C14E.toInt(), gravity = 4.5f)
            }
            Burst.XP -> repeat(8) {
                particles += Particle(hx + rnd.nextFloat() * 10 - 5, hy + 6, 0f, -0.9f - rnd.nextFloat(), 0f, 1.1f, 0xFFFFE17A.toInt(), glow = true)
            }
            Burst.HEAL -> repeat(10) {
                particles += Particle(hx + rnd.nextFloat() * 12 - 6, hy + 10, 0f, -0.8f - rnd.nextFloat() * 0.6f, 0f, 1.2f, 0xFF7CFFB2.toInt(), glow = true)
            }
            Burst.HURT -> {
                hurtUntil = now + 380
                repeat(8) {
                    particles += Particle(hx, hy + 2, rnd.nextFloat() * 2.4f - 1.2f, -1.2f - rnd.nextFloat(), 0f, 0.6f, 0xFFE5484D.toInt(), gravity = 5f)
                }
            }
            Burst.HIT, Burst.CRIT -> {
                attackUntil = now + 220
                bossFlashUntil = now + 160
                val bx = bossX.toFloat()
                val n = if (kind == Burst.CRIT) 18 else 9
                repeat(n) {
                    val a = rnd.nextFloat() * 2 * PI.toFloat()
                    val sp = 1f + rnd.nextFloat() * if (kind == Burst.CRIT) 2.2f else 1.2f
                    particles += Particle(bx, groundY - 12f, cos(a) * sp, sin(a) * sp, 0f, 0.5f, if (kind == Burst.CRIT) 0xFFFFB347.toInt() else 0xFFFFFFFF.toInt(), gravity = 2f, glow = kind == Burst.CRIT)
                }
            }
            Burst.LEVEL, Burst.VICTORY -> {
                levelUntil = now + 1400
                val colors = intArrayOf(0xFFFFE17A.toInt(), 0xFF7C5CFF.toInt(), 0xFF5EC8FF.toInt(), 0xFFF28DB2.toInt(), 0xFF7CFFB2.toInt())
                repeat(36) {
                    val a = it / 36f * 2 * PI.toFloat()
                    particles += Particle(hx, hy, cos(a) * 2.2f, sin(a) * 2.2f - 0.6f, 0f, 1.3f, colors[it % colors.size], gravity = 1.2f, glow = true)
                }
            }
            Burst.FIREWORKS -> {
                levelUntil = now + 1400
                val colors = intArrayOf(0xFFFFE17A.toInt(), 0xFFFF6B8B.toInt(), 0xFF5EC8FF.toInt(), 0xFF7CFFB2.toInt(), 0xFFB79CFF.toInt(), 0xFFFFB347.toInt())
                // Shells burst at once across the sky, in rings of different sizes.
                repeat(6) { shell ->
                    val cx = w * (0.12f + 0.76f * rnd.nextFloat())
                    val cy = h * (0.12f + 0.3f * rnd.nextFloat())
                    val col = colors[(shell + rnd.nextInt(colors.size)) % colors.size]
                    val speed = 1.2f + rnd.nextFloat() * 1.3f
                    repeat(22) {
                        val a = it / 22f * 2 * PI.toFloat()
                        particles += Particle(cx, cy, cos(a) * speed, sin(a) * speed, 0f, 1.1f + rnd.nextFloat() * 0.6f, if (it % 4 == 0) 0xFFFFFFFF.toInt() else col, gravity = 0.9f, glow = true)
                    }
                }
            }
            Burst.DROP -> repeat(12) {
                val a = rnd.nextFloat() * 2 * PI.toFloat()
                particles += Particle(hx, hy - 8, cos(a) * 1.4f, sin(a) * 1.4f, 0f, 0.8f, 0xFFB79CFF.toInt(), glow = true)
            }
        }
    }

    fun render(s: SceneState, now: Long) {
        val dt = if (lastT == 0L) 0.066f else ((now - lastT) / 1000f).coerceIn(0f, 0.2f)
        lastT = now
        val b = buffer
        val night = nightness(s.hour, s.kind)

        // Layout: the hero steps aside when a boss is on stage.
        standY = if (s.garden != null) groundY + GARDEN_PATH + 4 else groundY
        heroX = if (s.garden != null) (w * 0.55f).roundToInt() - Sprites.HERO_W / 2 else if (s.boss != null) (w * 0.27f).roundToInt() - Sprites.HERO_W / 2 else (w * 0.46f).roundToInt() - Sprites.HERO_W / 2
        bossX = (w * 0.74f).roundToInt()

        drawSky(s, now, night)
        drawFar(s, now, night)
        drawGround(s, now)
        if (s.garden != null) drawGarden(s.garden, now, night) else drawDecor(s, now, night)
        rays(s, now, night)
        drawActors(s, now)
        foreground(s, now)
        grade(s)
        if (night > 0f && s.kind != SceneKind.CAVE) shade(night)
        if (s.garden != null && night > 0f) gardenLights(s.garden, night)
        drawAmbient(s, now, dt, night)
        drawParticles(dt)
    }

    // ---- Sky -----------------------------------------------------------------------------

    private class SkyKey(val at: Float, val top: Int, val mid: Int, val low: Int)

    private val keys = listOf(
        SkyKey(0f, 0xFF0B0D24.toInt(), 0xFF1B1F4A.toInt(), 0xFF35305E.toInt()),
        SkyKey(4.5f, 0xFF0B0D24.toInt(), 0xFF1B1F4A.toInt(), 0xFF35305E.toInt()),
        SkyKey(6.2f, 0xFF3B4A8F.toInt(), 0xFFD9829E.toInt(), 0xFFFFC48A.toInt()),
        SkyKey(8.5f, 0xFF4A9BEF.toInt(), 0xFF86C4F4.toInt(), 0xFFD4ECFA.toInt()),
        SkyKey(16.5f, 0xFF4A9BEF.toInt(), 0xFF86C4F4.toInt(), 0xFFD4ECFA.toInt()),
        SkyKey(18.6f, 0xFF2F3374.toInt(), 0xFFC4688C.toInt(), 0xFFFFAE6B.toInt()),
        SkyKey(20.3f, 0xFF0B0D24.toInt(), 0xFF1B1F4A.toInt(), 0xFF35305E.toInt()),
        SkyKey(24f, 0xFF0B0D24.toInt(), 0xFF1B1F4A.toInt(), 0xFF35305E.toInt()),
    )

    private fun skyAt(hour: Float): Triple<Int, Int, Int> {
        val i = keys.indexOfLast { it.at <= hour }.coerceIn(0, keys.size - 2)
        val a = keys[i]
        val z = keys[i + 1]
        val t = ((hour - a.at) / (z.at - a.at)).coerceIn(0f, 1f)
        return Triple(Color.mix(a.top, z.top, t), Color.mix(a.mid, z.mid, t), Color.mix(a.low, z.low, t))
    }

    fun nightness(hour: Float, kind: SceneKind): Float {
        if (kind == SceneKind.CAVE) return 0f
        return when {
            hour < 4.5f || hour >= 20.3f -> 1f
            hour < 7f -> 1f - (hour - 4.5f) / 2.5f
            hour < 17.5f -> 0f
            else -> (hour - 17.5f) / 2.8f
        }.coerceIn(0f, 1f)
    }

    /** Where the sun (by day) or the moon (by night) sits on its arc. */
    private fun skyBody(hour: Float): Triple<Float, Float, Boolean> {
        val horizon = groundY - 6
        val isDay = hour in 5.5f..19.5f
        val p = if (isDay) (hour - 5.5f) / 14f else (((hour + 24f - 19.5f) % 24f) / 10f)
        return Triple(w * (0.1f + 0.8f * p), horizon - sin(p * PI.toFloat()) * (horizon * 0.75f), isDay)
    }

    /** 0 at noon and midnight, 1 at the heart of sunrise and sunset. */
    private fun golden(hour: Float): Float {
        val d = minOf(abs(hour - 6.6f), abs(hour - 18.4f))
        return (1f - d / 1.6f).coerceIn(0f, 1f)
    }

    private fun drawSky(s: SceneState, now: Long, night: Float) {
        val b = buffer
        if (s.kind == SceneKind.CAVE) {
            val stops = intArrayOf(0xFF120E22.toInt(), 0xFF1E1836.toInt(), 0xFF2B2350.toInt(), 0xFF3A2F66.toInt())
            for (y in 0 until groundY) for (x in 0 until w) b.px[y * w + x] = Bayer.gradient(stops, y / groundY.toFloat(), x, y)
            return
        }
        var (top, mid, low) = skyAt(s.hour)
        if (s.kind == SceneKind.VOLCANO) {
            top = Color.mix(top, 0xFF2A1022.toInt(), 0.6f)
            mid = Color.mix(mid, 0xFF6A2A3A.toInt(), 0.55f)
            low = Color.mix(low, 0xFFE0693D.toInt(), 0.55f)
        }
        val stops = intArrayOf(top, Color.mix(top, mid, 0.5f), mid, Color.mix(mid, low, 0.5f), low)
        val horizon = groundY - 6
        val skyBottom = if (s.kind == SceneKind.SKY) h else groundY
        for (y in 0 until skyBottom) {
            val t = (y / horizon.toFloat()).coerceIn(0f, 1f)
            for (x in 0 until w) b.px[y * w + x] = Bayer.gradient(stops, t, x, y)
        }
        val (cx, cy, isDay) = skyBody(s.hour)
        // Dawn and dusk pool warm light along the horizon around the sun.
        val warm = golden(s.hour)
        if (warm > 0f) {
            for (y in horizon - 22 until skyBottom) {
                if (y < 0) continue
                val vy = 1f - abs(y - horizon) / 22f
                for (x in 0 until w) {
                    val vx = 1f - (abs(x - cx) / (w * 0.7f)).coerceAtMost(1f)
                    val a = warm * vy * (0.35f + 0.65f * vx) * 0.55f
                    if (a > Bayer.at(x, y) * 0.5f) b.px[y * w + x] = Color.mix(b.px[y * w + x], 0xFFFF9A5A.toInt(), a)
                }
            }
        }
        // Stars, and now and then a shooting star.
        if (night > 0.2f) {
            val r = Random(42)
            repeat(w / 3) {
                val sx = r.nextInt(w)
                val sy = r.nextInt((horizon * 0.8f).toInt().coerceAtLeast(1))
                val phase = r.nextInt(4000)
                val tw = ((now + phase) / 700) % 5 == 0L
                val alpha = ((night - 0.2f) / 0.8f * if (tw) 120 else 230).toInt()
                val col = if (it % 5 == 0) 0xFFBFD4FF.toInt() else if (it % 7 == 0) 0xFFFFD8B0.toInt() else 0xFFFFF6E0.toInt()
                b.plot(sx, sy, Color.withAlpha(col, alpha))
                if (r.nextInt(9) == 0 && !tw) {
                    b.plot(sx + 1, sy, Color.withAlpha(col, alpha / 3)); b.plot(sx - 1, sy, Color.withAlpha(col, alpha / 3))
                    b.plot(sx, sy + 1, Color.withAlpha(col, alpha / 3)); b.plot(sx, sy - 1, Color.withAlpha(col, alpha / 3))
                }
            }
            val cycle = now % 14000
            if (night > 0.7f && cycle < 700) {
                val k = cycle / 700f
                val sx = w * 0.15f + k * w * 0.35f + (now / 14000 % 3) * w * 0.2f
                val sy = 4f + k * 10f
                for (t in 0..6) b.plot((sx - t * 1.6f).toInt(), (sy - t * 0.45f).toInt(), Color.withAlpha(0xFFFFF6E0.toInt(), (220 * (1 - t / 7f) * (1 - k)).toInt()))
            }
        }
        if (isDay) {
            val glowC = if (s.hour < 8 || s.hour > 17) 0xFFFFB070.toInt() else 0xFFFFF4C2.toInt()
            // Soft rays turning slowly around the sun
            val spin = now / 9000.0
            for (k in 0 until 8) {
                val a = spin + k * PI / 4
                for (d in 6..11) {
                    val px = cx + cos(a).toFloat() * d
                    val py = cy + sin(a).toFloat() * d
                    if (Bayer.at(px.toInt(), py.toInt()) < 0.5f) b.plot(px.toInt(), py.toInt(), Color.withAlpha(glowC, 70 - d * 5))
                }
            }
            disc(cx, cy, 9.5f, Color.withAlpha(glowC, 28), dither = true)
            disc(cx, cy, 7f, Color.withAlpha(glowC, 55), dither = true)
            disc(cx, cy, 5f, Color.withAlpha(glowC, 110))
            disc(cx, cy, 3.6f, 0xFFFFF8DC.toInt())
            disc(cx - 1f, cy - 1f, 1.6f, 0xFFFFFFFF.toInt())
        } else {
            disc(cx, cy, 8f, Color.withAlpha(0xFFBFC8FF.toInt(), 24), dither = true)
            disc(cx, cy, 5.5f, Color.withAlpha(0xFFBFC8FF.toInt(), 45))
            disc(cx, cy, 3.8f, 0xFFF1EEDC.toInt())
            b.plot(cx.toInt() - 1, cy.toInt(), 0xFFCFCBB8.toInt()); b.plot(cx.toInt(), cy.toInt() + 1, 0xFFCFCBB8.toInt()); b.plot(cx.toInt() + 1, cy.toInt() - 2, 0xFFDAD6C2.toInt())
            disc(cx + 1.9f, cy - 1.3f, 3.1f, Bayer.gradient(stops, (cy / horizon).coerceIn(0f, 1f), 0, 0))
        }
        // Clouds in three depths: far ones small and slow, near ones big and quick.
        val cloudTint = when {
            night > 0.5f -> 0xA0141838.toInt()
            warm > 0.2f -> Color.withAlpha(0xFFFF8A7A.toInt(), (warm * 110).toInt())
            s.kind == SceneKind.VOLCANO -> 0x806A3A4A.toInt()
            else -> 0
        }
        val clouds = if (s.kind == SceneKind.SKY) 6 else 4
        for (k in 0 until clouds) {
            val spr = Scenery.cloud(k)
            val span = w + spr.w
            val speed = 260 - (k % 3) * 70
            val x = (((now / speed) + k * 61) % span).toInt() - spr.w
            val y = if (s.kind == SceneKind.SKY && k >= 4) groundY + 2 + (k - 4) * 5 else 2 + (k % 3) * 7 + (k % 2) * 2
            b.blit(spr, x, y, tint = cloudTint, alpha = if (s.kind == SceneKind.DESERT) 140 else if (k % 3 == 2) 170 else 235)
        }
        // A few birds glide across on fine days.
        if (night < 0.3f && s.kind != SceneKind.VOLCANO) {
            for (k in 0..2) {
                val span = w + 40
                val bx = (((now / 90) + k * 17 + (k * 53)) % span).toInt() - 20 + k * 6
                val by = 9 + k * 3 + (sin(now / 900.0 + k) * 2).roundToInt()
                val up = ((now / 260 + k) % 2) == 0L
                val bc = 0xC0302848.toInt()
                b.plot(bx, by, bc)
                if (up) { b.plot(bx - 1, by - 1, bc); b.plot(bx + 1, by - 1, bc) } else { b.plot(bx - 1, by, bc); b.plot(bx + 1, by, bc) }
            }
        }
    }

    private fun disc(cx: Float, cy: Float, r: Float, c: Int, dither: Boolean = false) {
        for (y in (cy - r).toInt() - 1..(cy + r).toInt() + 1) for (x in (cx - r).toInt() - 1..(cx + r).toInt() + 1) {
            val d = (x + 0.5f - cx) * (x + 0.5f - cx) + (y + 0.5f - cy) * (y + 0.5f - cy)
            if (d <= r * r) {
                if (dither && Bayer.at(x, y) > 0.75f) continue
                buffer.plot(x, y, c)
            }
        }
    }

    // ---- Landscape -----------------------------------------------------------------------

    private fun ridge(x: Int, seed: Int, amp: Float, freq: Float): Float =
        (sin(x * freq + seed) * 0.6f + sin(x * freq * 2.3f + seed * 1.7f) * 0.3f + sin(x * freq * 5.1f + seed * 0.3f) * 0.1f) * amp

    private data class Land(val far: Int, val mid: Int, val ground: Int, val groundDark: Int, val grass: Int, val leaf: Int)

    private fun land(kind: SceneKind) = when (kind) {
        SceneKind.MEADOW -> Land(0xFF7FA7C9.toInt(), 0xFF5FA868.toInt(), 0xFF6DBA5A.toInt(), 0xFF4E9A4A.toInt(), 0xFF8BD36A.toInt(), 0xFF4FAE5A.toInt())
        SceneKind.FOREST -> Land(0xFF5B7F86.toInt(), 0xFF2F6B4A.toInt(), 0xFF3F8A4A.toInt(), 0xFF2E6E3D.toInt(), 0xFF5CAF5A.toInt(), 0xFF2E7A4A.toInt())
        SceneKind.SHORE -> Land(0xFF7FB7D9.toInt(), 0xFF3E8FC9.toInt(), 0xFFEBD5A0.toInt(), 0xFFD4B87C.toInt(), 0xFFF5E6BC.toInt(), 0xFF4FAE5A.toInt())
        SceneKind.SNOW -> Land(0xFF9FB4D9.toInt(), 0xFFC9D8F0.toInt(), 0xFFF1F5FC.toInt(), 0xFFD3DEF0.toInt(), 0xFFFFFFFF.toInt(), 0xFF2F6B5A.toInt())
        SceneKind.DESERT -> Land(0xFFE0A87E.toInt(), 0xFFE7BC7A.toInt(), 0xFFF0CF8F.toInt(), 0xFFDDB16C.toInt(), 0xFFF7E0A8.toInt(), 0xFF4E9F5A.toInt())
        SceneKind.CAVE -> Land(0xFF2A2246.toInt(), 0xFF3A2F66.toInt(), 0xFF4A3F72.toInt(), 0xFF3A3060.toInt(), 0xFF5E528C.toInt(), 0xFF7C5CFF.toInt())
        SceneKind.GROVE -> Land(0xFFA59ED9.toInt(), 0xFF6FAE7A.toInt(), 0xFF7CC66A.toInt(), 0xFF5CA556.toInt(), 0xFF9BDB7A.toInt(), 0xFFF5A3C7.toInt())
        SceneKind.CASTLE -> Land(0xFF8C9CC4.toInt(), 0xFF5E9466.toInt(), 0xFF6AAE58.toInt(), 0xFF4C8C48.toInt(), 0xFF88C866.toInt(), 0xFF4FAE5A.toInt())
        SceneKind.VOLCANO -> Land(0xFF6A3A4A.toInt(), 0xFF4A2E3E.toInt(), 0xFF4E3C48.toInt(), 0xFF3A2C38.toInt(), 0xFF6E5866.toInt(), 0xFF3A2C34.toInt())
        SceneKind.SKY -> Land(0xFFB4C8EE.toInt(), 0xFF8FB6E0.toInt(), 0xFF7CC66A.toInt(), 0xFF8A6A58.toInt(), 0xFF9BDB7A.toInt(), 0xFF4FAE5A.toInt())
    }

    /** One silhouette layer: lit on the faces turned toward the sun, hazier toward its foot. */
    private fun mountains(base: Int, height: (Int) -> Float, color: Int, haze: Int, snow: Float, light: Float = 0.22f, faces: Boolean = true, hazeK: Float = 0.55f) {
        val b = buffer
        val lit = Color.mix(color, 0xFFFFFFFF.toInt(), light)
        val dark = Color.mix(color, 0xFF1A1440.toInt(), 0.16f)
        for (x in 0 until w) {
            val fh = height(x)
            val top = (base - fh).toInt()
            // Smoothed slope: faces climbing to the right look toward the sun.
            val facing = (height(x + 2) - height(x - 2)) / 4f
            for (y in top.coerceAtLeast(0) until base) {
                val depth = y - top
                val band = if (faces) 99 else 2
                var c = when {
                    depth == 0 -> lit
                    depth < band && facing > 0.15f -> lit
                    depth < band && facing < -0.15f -> dark
                    else -> color
                }
                if (snow > 0f && depth < (snow * fh * 0.35f) + (noise(x, 3) * 2f).toInt() && fh > 8f) {
                    c = if (facing >= 0f) 0xFFF4F8FF.toInt() else 0xFFC9D3EA.toInt()
                }
                val hz = (depth / (base - top + 0.01f)) * hazeK
                b.px[y * w + x] = if (hz > Bayer.at(x, y) * 0.6f) Color.mix(c, haze, hz) else c
            }
        }
    }

    private fun drawFar(s: SceneState, now: Long, night: Float) {
        val b = buffer
        val l = land(s.kind)
        val (_, _, low) = skyAt(s.hour)
        val horizon = groundY
        if (s.kind == SceneKind.CAVE) {
            // Stalactites from the ceiling, rock pillars behind
            for (x in 0 until w) {
                val len = (abs(ridge(x, 3, 7f, 0.21f)) + if (x % 11 == 0) 6 else 0).toInt()
                for (y in 0..len) b.px[y * w + x] = when {
                    y == len -> 0xFF2F2650.toInt()
                    x % 11 == 1 && y > 1 -> 0xFF2A2248.toInt()
                    else -> 0xFF1A1530.toInt()
                }
                val top = (horizon - 18 + ridge(x, 9, 6f, 0.07f)).toInt()
                for (y in top until horizon) b.px[y * w + x] = if (y == top) 0xFF3E3470.toInt() else Bayer.gradient(intArrayOf(0xFF2E2656.toInt(), 0xFF241E44.toInt()), (y - top) / 18f, x, y)
            }
            return
        }
        val farC = Color.mix(l.far, low, 0.45f)
        val midC = Color.mix(l.mid, low, 0.2f)
        when (s.kind) {
            SceneKind.SHORE -> mountains(horizon - 4, { x -> 6 + ridge(x, 4, 3f, 0.05f) }, farC, low, 0f)
            SceneKind.DESERT -> mountains(horizon - 3, { x -> 10 + ridge(x, 2, 3f, 0.03f) + abs(ridge(x, 6, 2f, 0.09f)) }, farC, low, 0f, 0.3f)
            SceneKind.SKY -> {
                // Far islands drifting in the haze.
                listOf(0.18f to 7, 0.55f to 4, 0.83f to 9).forEachIndexed { k, (p, sz) ->
                    val ix = (w * p).toInt()
                    val iy = groundY - 22 - k * 3 + (sin(now / 1800.0 + k) * 1.2).roundToInt()
                    for (x in -sz * 2..sz * 2) {
                        val f = abs(x) / (sz * 2f)
                        val d = ((1 - f * f) * sz).toInt()
                        for (y in 0..d) b.plot(ix + x, iy + y, Color.mix(if (y == 0) 0xFF7CC66A.toInt() else 0xFF8A6A58.toInt(), low, 0.55f))
                    }
                }
                return
            }
            SceneKind.VOLCANO -> {
                mountains(horizon - 2, { x -> 8 + ridge(x, 5, 4f, 0.05f) }, farC, low, 0f, 0.12f)
                // The volcano itself: a cone with a glowing crater and lava running down.
                val vx = w * 0.62f
                val coneH = (groundY * 0.62f)
                for (x in 0 until w) {
                    val d = abs(x - vx)
                    val hgt = (coneH - d * 0.9f).coerceAtMost(coneH - 3f)
                    if (hgt <= 0) continue
                    val top = (horizon - 4 - hgt).toInt()
                    for (y in top.coerceAtLeast(0) until horizon - 4) {
                        val c = if (x < vx) 0xFF5A3444.toInt() else 0xFF3E2434.toInt()
                        b.px[y * w + x] = if (y == top) Color.mix(c, 0xFFFFFFFF.toInt(), 0.12f) else c
                    }
                }
                val craterY = (horizon - 4 - coneH + 3).toInt()
                val pulse = 0.6f + 0.4f * sin(now / 500.0).toFloat()
                for (k in 0..2) {
                    var x = vx + (k - 1) * 3f
                    var y = craterY.toFloat()
                    while (y < horizon - 4) {
                        b.plot(x.toInt(), y.toInt(), Color.mix(0xFFFF7A3D.toInt(), 0xFFFFE066.toInt(), noise(x.toInt(), (y + now / 200).toInt()) * pulse))
                        y += 1f
                        x += (noise(k, y.toInt()) - 0.5f) * 1.2f + (k - 1) * 0.35f
                    }
                }
                disc(vx, craterY.toFloat(), 4f, Color.withAlpha(0xFFFF9A3D.toInt(), (90 * pulse).toInt()), dither = true)
                // Smoke rising from the crater
                for (k in 0..5) {
                    val t = ((now / 60 + k * 37) % 220) / 220f
                    val sx = vx + sin(t * 5 + k) * 3 + t * 14
                    val sy = craterY - t * craterY
                    disc(sx, sy, 2f + t * 5f, Color.withAlpha(0xFF4A3A48.toInt(), (150 * (1 - t)).toInt()), dither = true)
                }
            }
            else -> {
                val snow = when (s.kind) {
                    SceneKind.SNOW -> 1f
                    SceneKind.MEADOW, SceneKind.CASTLE -> if (s.season == Season.WINTER) 0.8f else 0.45f
                    else -> if (s.season == Season.WINTER) 0.6f else 0f
                }
                mountains(horizon - 4, { x -> 15 + ridge(x, 1, 7f, 0.045f) + abs(ridge(x, 5, 4f, 0.11f)) }, farC, low, snow)
            }
        }
        if (s.kind == SceneKind.SHORE) {
            // The sea mirrors the sky; a column of light glitters under the sun or moon.
            val seaTop = horizon - 9
            val (sunX, _, isDay) = skyBody(s.hour)
            val water = Color.mix(0xFF3E8FC9.toInt(), low, 0.35f)
            val deep = Color.mix(0xFF2A6FB0.toInt(), low, 0.2f)
            for (y in seaTop until horizon) for (x in 0 until w) {
                var c = Bayer.gradient(intArrayOf(Color.mix(water, low, 0.4f), water, deep), (y - seaTop) / 9f, x, y)
                val wave = sin(x * 0.35 + y * 1.7 + now / 600.0)
                if (wave > 0.86) c = Color.mix(c, 0xFFFFFFFF.toInt(), 0.3f)
                val col = abs(x - sunX) < 3 + (y - seaTop) * 0.5f
                if (col && ((x + y + now / 180) % 3).toInt() == 0) c = Color.mix(c, if (isDay) 0xFFFFF4C2.toInt() else 0xFFE0E6FF.toInt(), 0.7f)
                b.px[y * w + x] = c
            }
            for (x in 0 until w) b.px[seaTop * w + x] = Color.mix(water, 0xFFFFFFFF.toInt(), 0.25f)
            return
        }
        // Nearer hills topped with trees
        val treeTop: (Int) -> Float = when (s.kind) {
            SceneKind.FOREST, SceneKind.SNOW -> { x -> val m = ((x * 3 + 7) % 9); 6f + ridge(x, 7, 3f, 0.08f) + (4 - abs(m - 4)) * 1.3f }
            SceneKind.DESERT -> { x -> 5 + ridge(x, 8, 3.5f, 0.06f) }
            SceneKind.VOLCANO -> { x -> 4 + abs(ridge(x, 8, 3f, 0.1f)) }
            else -> { x ->
                val bump = (x + 3) % 8
                5 + ridge(x, 7, 3f, 0.08f) + kotlin.math.sqrt((16f - (bump - 4f) * (bump - 4f)).coerceAtLeast(0f)) * 0.7f * (if (noise(x / 8, 2) > 0.3f) 1f else 0.2f)
            }
        }
        val midLit = if (s.kind == SceneKind.SNOW) 0.3f else 0.16f
        mountains(horizon - 1, treeTop, if (s.kind == SceneKind.SNOW) Color.mix(0xFF6F8FA8.toInt(), low, 0.35f) else midC, low, 0f, midLit, faces = false, hazeK = 0.2f)
        if (s.kind == SceneKind.CASTLE) {
            val spr = Scenery.castle(Color.mix(0xFFA6A2BC.toInt(), low, 0.25f), 0xFFE2435F.toInt(), night > 0.4f)
            b.blit(spr, (w * 0.5f).toInt() - spr.w / 2, horizon - spr.h - 3)
        }
    }

    private fun drawDecor(s: SceneState, now: Long, night: Float) {
        val b = buffer
        val l = land(s.kind)
        val g = groundY + 2
        fun at(p: Float, spr: Sprite, flip: Boolean = false, glow: Int = 0, sink: Int = 0) = b.blit(spr, (w * p).toInt() - spr.w / 2, g - spr.h + sink, flip = flip, glowAlpha = glow)
        val leaf = when (s.season) {
            Season.SPRING -> l.leaf
            Season.AUTUMN -> 0xFFE8862E.toInt()
            Season.WINTER -> 0xFFDDE8F5.toInt()
            else -> l.leaf
        }
        val leaf2 = when (s.season) {
            Season.AUTUMN -> 0xFFC8553D.toInt()
            Season.WINTER -> 0xFFC9D8F0.toInt()
            else -> Color.mix(l.leaf, 0xFF8BD36A.toInt(), 0.3f)
        }
        val blossom = if (s.season == Season.SPRING) 0xFFF5A3C7.toInt() else 0
        val winter = s.season == Season.WINTER
        when (s.kind) {
            SceneKind.MEADOW -> {
                at(0.05f, Scenery.oak(leaf, 0, blossom))
                at(0.92f, Scenery.oak(leaf2, 1, blossom), flip = true)
                at(0.15f, Scenery.fence(), sink = 1)
                when (s.season) {
                    Season.AUTUMN -> at(0.6f, Sprites.pumpkins(), sink = 1)
                    Season.WINTER -> at(0.6f, Sprites.snowdrift(), sink = 1)
                    else -> at(0.6f, Scenery.bush(0xFF5DAE4F.toInt(), 0xFFE2435F.toInt()), sink = 1)
                }
                at(0.37f, Scenery.rock(0xFF9AA0B5.toInt(), moss = !winter), sink = 1)
            }
            SceneKind.FOREST -> {
                listOf(0.0f to 1, 0.13f to 0, 0.58f to 1, 0.86f to 0, 1.0f to 1).forEachIndexed { i, (p, tall) ->
                    at(p, Scenery.pine(if (i % 2 == 0) l.leaf else 0xFF3A8A5A.toInt(), winter, tall), sink = 1)
                }
                at(0.38f, Scenery.mushroom(0xFFE2435F.toInt()), sink = 1)
                at(0.66f, Scenery.log(), sink = 1)
                at(0.95f, Scenery.mushroom(0xFFE8862E.toInt()), sink = 1)
            }
            SceneKind.SNOW -> {
                listOf(0.04f to 1, 0.9f to 0, 0.62f to 0).forEach { (p, t) -> at(p, Scenery.pine(l.leaf, true, t), sink = 1) }
                at(0.38f, Scenery.rock(0xFFC9D3EA.toInt()), sink = 1)
                at(0.8f, Sprites.snowdrift(), sink = 1)
            }
            SceneKind.DESERT -> {
                at(0.08f, Scenery.cactus())
                at(0.9f, Scenery.cactus(), flip = true)
                at(0.62f, Scenery.rock(0xFFC08A5A.toInt(), big = true), sink = 1)
                at(0.38f, Scenery.rock(0xFFB07A4A.toInt()), sink = 1)
            }
            SceneKind.SHORE -> {
                at(0.06f, Scenery.palm())
                at(0.92f, Scenery.palm(), flip = true)
                at(0.62f, Scenery.rock(0xFF8E94A8.toInt(), moss = true, big = true), sink = 1)
                // Foam rolling up the sand
                val foamY = groundY
                for (x in 0 until w) {
                    val reach = (sin(x * 0.12 + now / 900.0) * 1.2 + sin(now / 1400.0) * 0.8).roundToInt()
                    if (reach >= 0) b.plot(x, foamY - 1 + reach.coerceAtMost(1), 0xE0FFFFFF.toInt())
                }
            }
            SceneKind.CAVE -> {
                val pulse = (120 + 80 * sin(now / 700.0)).toInt()
                listOf(0.04f to 0xFF5EC8FF.toInt(), 0.62f to 0xFFE070FF.toInt(), 0.9f to 0xFF7CFFB2.toInt()).forEach { (p, c) ->
                    at(p, Scenery.crystal(c), glow = pulse, sink = 1)
                }
                at(0.38f, Scenery.rock(0xFF5E528C.toInt()), sink = 1)
            }
            SceneKind.GROVE -> {
                at(0.06f, Scenery.oak(0xFFF5A3C7.toInt(), 2, 0xFFFFE0EC.toInt()))
                at(0.9f, Scenery.oak(0xFFF7B8D2.toInt(), 0, 0xFFFFFFFF.toInt()), flip = true)
                at(0.62f, Scenery.stoneLantern(night > 0.4f), glow = if (night > 0.4f) 200 else 0, sink = 1)
                at(0.37f, Scenery.bush(0xFF5DAE4F.toInt(), 0xFFF5A3C7.toInt()), sink = 1)
            }
            SceneKind.CASTLE -> {
                at(0.05f, Scenery.ruinPillar(0xFFA6A2BC.toInt(), false), sink = 1)
                at(0.14f, Scenery.ruinPillar(0xFFA6A2BC.toInt(), true), sink = 1)
                at(0.93f, Scenery.oak(leaf, 2, blossom), flip = true)
                at(0.62f, Scenery.banner(0xFF4F7BD9.toInt(), ((now / 400) % 2).toInt()), sink = 1)
                at(0.37f, Scenery.rock(0xFFA6A2BC.toInt(), moss = true), sink = 1)
            }
            SceneKind.VOLCANO -> {
                at(0.06f, Scenery.deadTree())
                at(0.92f, Scenery.deadTree(), flip = true)
                at(0.38f, Scenery.rock(0xFF3A2C3A.toInt()), sink = 1)
                at(0.62f, Scenery.rock(0xFF2E2434.toInt(), big = true), sink = 1)
            }
            SceneKind.SKY -> {
                at(0.12f, Scenery.oak(leaf, 1, blossom))
                at(0.88f, Scenery.crystal(0xFF8FD8FF.toInt()), glow = (120 + 80 * sin(now / 700.0)).toInt(), sink = 1)
                at(0.62f, Scenery.bush(0xFF5DAE4F.toInt(), 0xFFFFE066.toInt()), sink = 1)
            }
        }
    }

    private fun drawGround(s: SceneState, now: Long) {
        val b = buffer
        val winterGrass = s.season == Season.WINTER && s.kind in setOf(SceneKind.MEADOW, SceneKind.FOREST, SceneKind.CASTLE, SceneKind.GROVE, SceneKind.SKY)
        val l = land(s.kind).let {
            if (winterGrass) it.copy(ground = 0xFFEEF3FB.toInt(), groundDark = 0xFFD3DEF0.toInt(), grass = 0xFFFFFFFF.toInt())
            else it
        }
        val island = s.kind == SceneKind.SKY
        val bottom = if (island) groundY + 4 else h
        val light = Color.mix(l.ground, l.grass, 0.45f)
        for (y in groundY until bottom) for (x in 0 until w) {
            val t = (y - groundY) / (h - groundY).toFloat()
            var c = Bayer.gradient(intArrayOf(l.ground, l.groundDark), t * 0.9f, x, y)
            // Soft patches of lighter and darker ground so it reads as grass, sand or snow.
            val n = smoothNoise(x / 6f, y * 7)
            if (n > 0.72f && y > groundY + 1) c = if (Bayer.at(x, y) < 0.6f) light else c
            if (n < 0.22f && y > groundY + 2) c = l.groundDark
            b.px[y * w + x] = c
        }
        if (island) {
            val spr = Scenery.islandRock(w + 8, h - groundY - 2)
            b.blit(spr, -5, groundY + 3)
        }
        // A lit lip along the edge, with blades of grass that sway.
        val blades = s.kind !in setOf(SceneKind.CAVE, SceneKind.DESERT, SceneKind.VOLCANO, SceneKind.SHORE, SceneKind.SNOW) && !winterGrass
        val grassTop = Color.mix(l.grass, 0xFFFFFFFF.toInt(), 0.2f)
        for (x in 0 until w) {
            b.px[groundY * w + x] = l.grass
            if (blades) {
                val tall = (noise(x, 11) * 3.4f).toInt()
                val sway = if (tall >= 2 && sin(now / 700.0 + x * 0.35) > 0.55) 1 else 0
                for (k in 1..tall) b.plot(x + if (k == tall) sway else 0, groundY - k, if (k == tall) grassTop else l.grass)
            } else if ((x * 5 + 3) % 7 == 0 && s.kind != SceneKind.CAVE && s.kind != SceneKind.DESERT && s.kind != SceneKind.VOLCANO) b.plot(x, groundY - 1, l.grass)
        }
        // Little details
        val r = Random(3)
        repeat(w / 6) {
            val x = r.nextInt(w)
            val y = groundY + 2 + r.nextInt((bottom - groundY - 3).coerceAtLeast(1))
            when (s.kind) {
                SceneKind.MEADOW, SceneKind.GROVE, SceneKind.CASTLE, SceneKind.SKY -> if (winterGrass) b.plot(x, y, 0xFFFFFFFF.toInt()) else {
                    val fc = (if (s.kind == SceneKind.GROVE) intArrayOf(0xFFFFC6DD.toInt(), 0xFFF28DB2.toInt(), 0xFFFFFFFF.toInt(), 0xFFF5A3C7.toInt())
                    else intArrayOf(0xFFFFFFFF.toInt(), 0xFFF28DB2.toInt(), 0xFFFFE066.toInt(), 0xFFB79CFF.toInt()))[it % 4]
                    if (it % 3 == 0) {
                        b.plot(x, y, fc); b.plot(x - 1, y, fc); b.plot(x + 1, y, fc); b.plot(x, y - 1, fc); b.plot(x, y, 0xFFFFE066.toInt())
                        b.plot(x, y + 1, 0xFF3E8A40.toInt())
                    } else { b.plot(x, y, fc); b.plot(x, y + 1, 0xFF3E8A40.toInt()) }
                }
                SceneKind.FOREST -> { b.plot(x, y, 0xFF2A5A34.toInt()); b.plot(x + 1, y, 0xFF2A5A34.toInt()); if (it % 4 == 0) b.plot(x, y - 1, 0xFF6DBA5A.toInt()) }
                SceneKind.SNOW -> b.plot(x, y, if ((now / 400 + it) % 9 == 0L) 0xFFBFE6FF.toInt() else 0xFFFFFFFF.toInt())
                SceneKind.DESERT -> { b.plot(x, y, 0xFFC99A5B.toInt()); b.plot(x + 1, y, 0xFFF7E0A8.toInt()) }
                SceneKind.SHORE -> if (it % 3 == 0) { b.plot(x, y, 0xFFF28DB2.toInt()); b.plot(x + 1, y, 0xFFFFD1E0.toInt()) } else b.plot(x, y, 0xFFC9A970.toInt())
                SceneKind.CAVE -> b.plot(x, y, if (it % 4 == 0) 0xFF7CFFB2.toInt() else 0xFF6A5EA0.toInt())
                SceneKind.VOLCANO -> {
                    // Lava cracks that breathe
                    val glow = (0.5f + 0.5f * sin(now / 450.0 + it)).toFloat()
                    val c = Color.mix(0xFFB8402A.toInt(), 0xFFFFC94A.toInt(), glow)
                    b.plot(x, y, c); b.plot(x + 1, y, c); if (it % 2 == 0) b.plot(x + 2, y + 1, c)
                }
            }
        }
        if (s.kind == SceneKind.DESERT) {
            // Wind ripples in the sand
            for (y in groundY + 3 until h step 3) for (x in 0 until w) if ((x + y * 3) % 11 < 3 && noise(x / 9, y) > 0.4f) b.plot(x, y, 0x60FFF4D0)
        }
    }

    /** Sunbeams slanting through the leaves on fine forest days. */
    private fun rays(s: SceneState, now: Long, night: Float) {
        if (night > 0.3f || s.kind !in setOf(SceneKind.FOREST, SceneKind.GROVE)) return
        val drift = (now / 220).toInt()
        for (y in 0 until groundY + 4) for (x in 0 until w) {
            val band = Math.floorMod(x + y / 2 + drift, 47)
            if (band < 5 && Bayer.at(x, y) < 0.35f) buffer.plot(x, y, 0x26FFF4C2)
        }
    }

    /** Grass in front of everyone, for depth. */
    private fun foreground(s: SceneState, now: Long) {
        if (s.kind in setOf(SceneKind.CAVE, SceneKind.SKY, SceneKind.VOLCANO)) return
        val winterGrass = s.season == Season.WINTER && s.kind in setOf(SceneKind.MEADOW, SceneKind.FOREST, SceneKind.CASTLE, SceneKind.GROVE)
        val l = land(s.kind)
        val dark = if (winterGrass || s.kind == SceneKind.SNOW) 0xFFC9D6EC.toInt() else if (s.kind == SceneKind.DESERT || s.kind == SceneKind.SHORE) Color.mix(l.groundDark, 0xFF8A5A36.toInt(), 0.25f) else Color.mix(l.groundDark, 0xFF1E3A2A.toInt(), 0.3f)
        for (k in 0 until w / 14) {
            val x0 = (noise(k, 41) * w).toInt()
            val n = 3 + (noise(k, 43) * 4).toInt()
            for (j in 0 until n) {
                val x = x0 + j - n / 2
                val hgt = 1 + ((1f - abs(j - n / 2f) / n) * 4 * noise(x, 47)).toInt()
                val sway = if (hgt >= 3 && sin(now / 650.0 + x * 0.4) > 0.6) 1 else 0
                for (y in 0 until hgt) buffer.plot(x + if (y == hgt - 1) sway else 0, h - 1 - y, dark)
            }
        }
    }

    /** Dawn and dusk paint the land warm. */
    private fun grade(s: SceneState) {
        val warm = golden(s.hour) * if (s.kind == SceneKind.CAVE) 0f else 0.22f
        if (warm <= 0f) return
        val tint = 0xFFFF8A4A.toInt()
        for (y in groundY - 30 until h) {
            if (y < 0) continue
            for (x in 0 until w) {
                val i = y * w + x
                buffer.px[i] = Color.mix(buffer.px[i], tint, warm)
            }
        }
    }

    // ---- Actors --------------------------------------------------------------------------

    private fun shadow(cx: Int, width: Int) {
        for (x in cx - width / 2..cx + width / 2) for (y in standY + 1..standY + 2) {
            val edge = x == cx - width / 2 || x == cx + width / 2
            if (edge && y == standY + 2) continue
            buffer.plot(x, y, 0x55000018)
        }
    }

    private fun drawActors(s: SceneState, now: Long) {
        val b = buffer
        val look = s.look ?: return
        val bob = if ((now / 520) % 2 == 1L) 1 else 0
        val blink = now % 3400 < 140
        val lunge = if (now < attackUntil) 3 else 0
        val hurtShake = if (now < hurtUntil) (if ((now / 50) % 2 == 0L) -1 else 1) else 0
        val hero = Sprites.hero(look, blink)
        val glow = (150 + 80 * sin(now / 500.0)).toInt()
        val tint = when {
            now < hurtUntil -> 0x99E5484D.toInt()
            now < levelUntil && (now / 120) % 2 == 0L -> 0x55FFF3B0
            else -> 0
        }
        val mounted = Catalog.petParts(s.mount)
        // Riding: the beast stands where the hero would, and the hero sits on it, seen from the front:
        // head and shoulders above the beast's head, legs hidden behind it.
        val mount = mounted?.let { (sp, po) -> Sprites.mount(sp, po, blink = (now + 1700) % 5300 < 160) }
        val hx = heroX + lunge + hurtShake
        val mx = mount?.let { hx + hero.w / 2 - it.w / 2 } ?: 0
        val my = mount?.let { standY - it.h + 3 + bob } ?: 0
        val hy = if (mount != null) my + topRow(mount) - RIDER_LIFT else standY - hero.h + 2 + bob
        // The companion trots alongside, beside the hero or the beast.
        val right = if (mount != null) mx + mount.w - 5 else hx + hero.w - 2
        val parts = Catalog.petParts(s.pet)
        if (parts != null) {
            val (sp, po) = parts
            val pet = Sprites.pet(sp, po, s.petGrown, blink = (now + 900) % 4100 < 140)
            val hopT = (now % 2600)
            val hop = if (hopT < 300) (sin(hopT / 300.0 * PI) * 3).roundToInt() else 0
            val px = if (s.boss != null && mount == null) hx - pet.w + 3 else right
            val py = standY - pet.h + 2 - hop
            shadow(px + pet.w / 2, 8)
            b.blit(pet, px, py, flip = s.boss == null, glowAlpha = if (s.petGrown) (140 + 60 * sin(now / 400.0)).toInt() else 0)
        }
        if (mount != null) {
            shadow(mx + mount.w / 2, 20)
            b.blit(hero, hx, hy, tint = tint, glowAlpha = glow)
            b.blit(mount, mx, my, tint = if (now < hurtUntil) 0x40E5484D else 0)
        } else {
            shadow(hx + hero.w / 2, 10)
            b.blit(hero, hx, hy, tint = tint, glowAlpha = glow)
        }
        if (s.resting) {
            val z = (now / 600) % 3
            val zc = 0xE0FFFFFF.toInt()
            for (k in 0..z.toInt()) {
                val zx = hx + hero.w - 3 + k * 3
                val zy = hy + 3 - k * 4
                b.plot(zx, zy, zc); b.plot(zx + 1, zy, zc); b.plot(zx + 1, zy + 1, zc); b.plot(zx, zy + 2, zc); b.plot(zx + 1, zy + 2, zc)
            }
        }
        // Boss
        s.boss?.let { boss ->
            val frame = ((now / 650) % 2).toInt()
            val spr = Sprites.boss(boss.shape, boss.primary, boss.secondary, frame)
            val bx = bossX - spr.w / 2
            val by = standY - spr.h + 3
            shadow(bossX, spr.w - 8)
            b.blit(spr, bx, by, flip = boss.shape == app.dayquest.game.BossShape.WYRM || boss.shape == app.dayquest.game.BossShape.DRAGON, tint = if (now < bossFlashUntil) 0xCCFFFFFF.toInt() else 0, glowAlpha = glow)
        }
    }

    /** Night falls over the land (not the sky, which already has its own colours). */
    private fun shade(night: Float) {
        val k = night * 0.42f
        val tintC = 0xFF141A3C.toInt()
        for (y in groundY - 30 until h) {
            if (y < 0) continue
            for (x in 0 until w) {
                val i = y * w + x
                buffer.px[i] = Color.mix(buffer.px[i], tintC, k)
            }
        }
    }

    // ---- Ambient life & particles --------------------------------------------------------

    private fun drawAmbient(s: SceneState, now: Long, dt: Float, night: Float) {
        val falling = fallingSeason(s)
        val want = if (falling != null && night < 0.5f) 14 else when (s.kind) {
            SceneKind.SNOW -> 26
            SceneKind.VOLCANO -> 16
            SceneKind.GROVE -> 14
            SceneKind.FOREST, SceneKind.CAVE -> 10
            SceneKind.MEADOW, SceneKind.CASTLE -> if (night > 0.5f) 9 else 4
            SceneKind.SHORE, SceneKind.DESERT, SceneKind.SKY -> if (night > 0.5f) 4 else 2
        }
        while (ambient.size < want) ambient += spawnAmbient(s, night, fresh = ambient.isEmpty())
        val it = ambient.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            p.x += p.vx * dt * 10
            p.y += p.vy * dt * 10
            if (s.kind == SceneKind.SNOW || p.max >= 20f) p.x += sin((now + p.max * 1000) / 600.0).toFloat() * 0.05f
            else p.vy += (rnd.nextFloat() - 0.5f) * 0.3f * dt * 10
            if (p.life > p.max || p.y > h || p.x < -2 || p.x > w + 2) { it.remove(); continue }
            val fade = minOf(1f, p.life * 2, (p.max - p.life) * 2)
            val c = Color.withAlpha(p.color, (Color.a(p.color) * fade).toInt())
            if (p.glow) {
                val pulse = 0.6f + 0.4f * sin((now + p.max * 997) / 250.0).toFloat()
                val gc = Color.withAlpha(p.color, (60 * fade * pulse).toInt())
                buffer.plot(p.x.toInt() - 1, p.y.toInt(), gc); buffer.plot(p.x.toInt() + 1, p.y.toInt(), gc)
                buffer.plot(p.x.toInt(), p.y.toInt() - 1, gc); buffer.plot(p.x.toInt(), p.y.toInt() + 1, gc)
                buffer.plot(p.x.toInt(), p.y.toInt(), Color.withAlpha(c, (Color.a(c) * pulse).toInt()))
            } else {
                buffer.plot(p.x.toInt(), p.y.toInt(), c)
            }
        }
    }

    /** Seasons that fill the air: spring petals, autumn leaves, winter snow (not in caves or deserts). */
    private fun fallingSeason(s: SceneState): Season? =
        s.season?.takeIf { it != Season.SUMMER && s.kind !in setOf(SceneKind.CAVE, SceneKind.DESERT, SceneKind.SNOW, SceneKind.VOLCANO, SceneKind.GROVE) }

    private fun spawnAmbient(s: SceneState, night: Float, fresh: Boolean): Particle {
        val x = rnd.nextFloat() * w
        val falling = fallingSeason(s)
        if (falling != null && night < 0.5f) {
            val color = when (falling) {
                Season.SPRING -> if (rnd.nextBoolean()) 0xFFFFC6DD.toInt() else 0xFFFFFFFF.toInt()
                Season.AUTUMN -> intArrayOf(0xFFE8862E.toInt(), 0xFFC8553D.toInt(), 0xFFF2C14E.toInt())[rnd.nextInt(3)]
                else -> 0xF0FFFFFF.toInt()
            }
            val speed = if (falling == Season.WINTER) 0.5f else 0.3f
            return Particle(x, if (fresh) rnd.nextFloat() * h else -1f, if (falling == Season.WINTER) -0.1f else 0.25f, speed + rnd.nextFloat() * 0.3f, 0f, 24f, color)
        }
        return when {
            s.kind == SceneKind.SNOW -> Particle(x, if (fresh) rnd.nextFloat() * h else -1f, -0.1f, 0.5f + rnd.nextFloat() * 0.4f, 0f, 30f, 0xF0FFFFFF.toInt())
            s.kind == SceneKind.VOLCANO -> Particle(x, groundY + rnd.nextFloat() * 8, (rnd.nextFloat() - 0.5f) * 0.3f, -0.4f - rnd.nextFloat() * 0.5f, 0f, 5f + rnd.nextFloat() * 4, if (rnd.nextBoolean()) 0xFFFF9A3D.toInt() else 0xFFFFE066.toInt(), glow = true)
            s.kind == SceneKind.GROVE && night < 0.5f -> Particle(x, if (fresh) rnd.nextFloat() * h else -1f, 0.3f, 0.3f + rnd.nextFloat() * 0.3f, 0f, 24f, if (rnd.nextBoolean()) 0xFFFFC6DD.toInt() else 0xFFF5A3C7.toInt())
            s.kind == SceneKind.CAVE -> Particle(x, groundY + rnd.nextFloat() * 6, 0f, -0.15f - rnd.nextFloat() * 0.2f, 0f, 6f + rnd.nextFloat() * 4, if (rnd.nextBoolean()) 0xFF5EC8FF.toInt() else 0xFFE070FF.toInt(), glow = true)
            night > 0.5f -> Particle(x, groundY - 4 - rnd.nextFloat() * 22, (rnd.nextFloat() - 0.5f) * 0.3f, 0f, 0f, 4f + rnd.nextFloat() * 4, 0xFFE8FF7A.toInt(), glow = true)
            s.kind == SceneKind.FOREST -> Particle(x, -1f, 0.2f, 0.35f, 0f, 20f, if (rnd.nextBoolean()) 0xFFE0A040.toInt() else 0xFFC8553D.toInt())
            else -> Particle(x, groundY - 6 - rnd.nextFloat() * 14, (rnd.nextFloat() - 0.5f) * 0.6f, 0f, 0f, 5f + rnd.nextFloat() * 4, intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFE066.toInt(), 0xFFF28DB2.toInt())[rnd.nextInt(3)])
        }
    }

    private fun drawParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            if (p.life >= p.max) { it.remove(); continue }
            p.vy += p.gravity * dt
            p.x += p.vx * dt * 14
            p.y += p.vy * dt * 14
            val a = (255 * (1 - p.life / p.max * p.life / p.max)).toInt()
            val c = Color.withAlpha(p.color, a)
            if (p.glow) {
                val gc = Color.withAlpha(p.color, a / 4)
                buffer.plot(p.x.toInt() - 1, p.y.toInt(), gc); buffer.plot(p.x.toInt() + 1, p.y.toInt(), gc)
                buffer.plot(p.x.toInt(), p.y.toInt() - 1, gc); buffer.plot(p.x.toInt(), p.y.toInt() + 1, gc)
            }
            buffer.plot(p.x.toInt(), p.y.toInt(), c)
        }
    }
}
