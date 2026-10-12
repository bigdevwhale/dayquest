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
)

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val max: Float, val color: Int, val gravity: Float = 0f, val size: Int = 1, val glow: Boolean = false,
)

/** Kinds of moments the scene reacts to. */
enum class Burst { GOLD, XP, HIT, CRIT, HURT, LEVEL, DROP, HEAL, VICTORY }

class SceneRenderer(val w: Int, val h: Int) {
    val buffer = PixelBuffer(w, h)
    private val particles = ArrayList<Particle>()
    private val ambient = ArrayList<Particle>()
    private val rnd = Random(7)
    private var hurtUntil = 0L
    private var attackUntil = 0L
    private var bossFlashUntil = 0L
    private var levelUntil = 0L
    private var lastT = 0L

    val groundY = h - 14

    // Actor anchors (feet), recomputed per state.
    private var heroX = 0
    private var bossX = 0

    fun heroCenter() = heroX + Sprites.HERO_W / 2f + 1

    fun burst(kind: Burst, now: Long) {
        val hx = heroX + Sprites.HERO_W / 2f + 1
        val hy = groundY - 14f
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
        heroX = if (s.boss != null) (w * 0.27f).roundToInt() - Sprites.HERO_W / 2 else (w * 0.46f).roundToInt() - Sprites.HERO_W / 2
        bossX = (w * 0.74f).roundToInt()

        drawSky(s, now, night)
        drawFar(s, night)
        drawDecor(s, now, night)
        drawGround(s, now)
        drawActors(s, now)
        if (night > 0f && s.kind != SceneKind.CAVE) shade(night)
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

    private fun drawSky(s: SceneState, now: Long, night: Float) {
        val b = buffer
        if (s.kind == SceneKind.CAVE) {
            val stops = intArrayOf(0xFF120E22.toInt(), 0xFF1E1836.toInt(), 0xFF2B2350.toInt(), 0xFF3A2F66.toInt())
            for (y in 0 until groundY) for (x in 0 until w) b.px[y * w + x] = Bayer.gradient(stops, y / groundY.toFloat(), x, y)
            return
        }
        val (top, mid, low) = skyAt(s.hour)
        val stops = intArrayOf(top, Color.mix(top, mid, 0.5f), mid, Color.mix(mid, low, 0.5f), low)
        val horizon = groundY - 6
        for (y in 0 until groundY) {
            val t = (y / horizon.toFloat()).coerceIn(0f, 1f)
            for (x in 0 until w) b.px[y * w + x] = Bayer.gradient(stops, t, x, y)
        }
        // Stars
        if (night > 0.2f) {
            val r = Random(42)
            repeat(w / 3) {
                val sx = r.nextInt(w)
                val sy = r.nextInt((horizon * 0.8f).toInt().coerceAtLeast(1))
                val phase = r.nextInt(4000)
                val tw = ((now + phase) / 700) % 5 == 0L
                val alpha = ((night - 0.2f) / 0.8f * if (tw) 120 else 230).toInt()
                b.plot(sx, sy, Color.withAlpha(0xFFFFF6E0.toInt(), alpha))
                if (r.nextInt(9) == 0 && !tw) {
                    b.plot(sx + 1, sy, Color.withAlpha(0xFFFFF6E0.toInt(), alpha / 3)); b.plot(sx - 1, sy, Color.withAlpha(0xFFFFF6E0.toInt(), alpha / 3))
                    b.plot(sx, sy + 1, Color.withAlpha(0xFFFFF6E0.toInt(), alpha / 3)); b.plot(sx, sy - 1, Color.withAlpha(0xFFFFF6E0.toInt(), alpha / 3))
                }
            }
        }
        // Sun or moon along an arc
        val isDay = s.hour in 5.5f..19.5f
        val p = if (isDay) (s.hour - 5.5f) / 14f else (((s.hour + 24f - 19.5f) % 24f) / 10f)
        val cx = (w * (0.1f + 0.8f * p))
        val cy = horizon - sin(p * PI.toFloat()) * (horizon * 0.75f)
        if (isDay) {
            val glowC = if (s.hour < 8 || s.hour > 17) 0xFFFFB070.toInt() else 0xFFFFF4C2.toInt()
            disc(cx, cy, 7.5f, Color.withAlpha(glowC, 50), dither = true)
            disc(cx, cy, 5.5f, Color.withAlpha(glowC, 90), dither = true)
            disc(cx, cy, 3.6f, 0xFFFFF8DC.toInt())
        } else {
            disc(cx, cy, 6f, Color.withAlpha(0xFFBFC8FF.toInt(), 40), dither = true)
            disc(cx, cy, 3.6f, 0xFFF1EEDC.toInt())
            disc(cx + 1.6f, cy - 1.2f, 3.2f, Bayer.gradient(stops, (cy / horizon).coerceIn(0f, 1f), 0, 0))
        }
        // Clouds
        val cloudTint = if (night > 0.5f) 0xA0141838.toInt() else if (s.hour < 8.5f || s.hour > 16.5f) 0x50FF9A7A else 0
        for (k in 0..2) {
            val spr = Sprites.cloud(k % 2)
            val span = w + spr.w
            val x = (((now / (180 + k * 70)) + k * 61) % span).toInt() - spr.w
            val y = 4 + k * 7 + (k % 2) * 3
            b.blit(spr, x, y, tint = cloudTint, alpha = if (s.kind == SceneKind.DESERT) 150 else 235)
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
    }

    private fun drawFar(s: SceneState, night: Float) {
        val b = buffer
        val l = land(s.kind)
        val (_, _, low) = skyAt(s.hour)
        val horizon = groundY
        if (s.kind == SceneKind.CAVE) {
            // Stalactites from the ceiling, rock pillars behind
            for (x in 0 until w) {
                val len = (abs(ridge(x, 3, 7f, 0.21f)) + if (x % 11 == 0) 6 else 0).toInt()
                for (y in 0..len) b.px[y * w + x] = if (y == len) 0xFF2F2650.toInt() else 0xFF1A1530.toInt()
                val top = (horizon - 18 + ridge(x, 9, 6f, 0.07f)).toInt()
                for (y in top until horizon) b.px[y * w + x] = if (y == top) 0xFF3E3470.toInt() else Bayer.gradient(intArrayOf(0xFF2E2656.toInt(), 0xFF241E44.toInt()), (y - top) / 18f, x, y)
            }
            return
        }
        val farC = Color.mix(l.far, low, 0.45f)
        val farTop = Color.mix(farC, 0xFFFFFFFF.toInt(), 0.25f)
        val midC = Color.mix(l.mid, low, 0.2f)
        for (x in 0 until w) {
            val fh = when (s.kind) {
                SceneKind.SNOW, SceneKind.MEADOW, SceneKind.FOREST -> 15 + ridge(x, 1, 7f, 0.045f) + abs(ridge(x, 5, 4f, 0.11f))
                SceneKind.DESERT -> 10 + ridge(x, 2, 3f, 0.03f)
                SceneKind.SHORE -> 6 + ridge(x, 4, 3f, 0.05f)
                SceneKind.CAVE -> 0f
            }
            val top = (horizon - 4 - fh).toInt()
            for (y in top until horizon) {
                val snowcap = s.kind == SceneKind.SNOW || (s.kind == SceneKind.MEADOW && y - top < 2 && fh > 19)
                b.px[y * w + x] = when {
                    y == top -> farTop
                    snowcap && y - top < 4 -> Color.mix(0xFFF4F8FF.toInt(), farC, (y - top) / 5f)
                    else -> farC
                }
            }
        }
        if (s.kind == SceneKind.SHORE) {
            // The sea: a band of water with drifting glints.
            val seaTop = horizon - 8
            val water = Color.mix(0xFF3E8FC9.toInt(), low, 0.25f)
            val deep = Color.mix(0xFF2A6FB0.toInt(), low, 0.2f)
            for (y in seaTop until horizon) for (x in 0 until w) {
                b.px[y * w + x] = Bayer.gradient(intArrayOf(water, deep), (y - seaTop) / 8f, x, y)
            }
            return
        }
        for (x in 0 until w) {
            val mh = when (s.kind) {
                SceneKind.DESERT -> 5 + ridge(x, 8, 3.5f, 0.06f)
                else -> 5 + ridge(x, 7, 3f, 0.08f)
            }
            val top = (horizon - 1 - mh).toInt()
            for (y in top until horizon) b.px[y * w + x] = if (y == top) Color.mix(midC, 0xFFFFFFFF.toInt(), 0.18f) else midC
        }
    }

    private fun drawDecor(s: SceneState, now: Long, night: Float) {
        val b = buffer
        val l = land(s.kind)
        val g = groundY
        val positions = listOf(0.04f, 0.17f, 0.62f, 0.9f)
        when (s.kind) {
            SceneKind.MEADOW -> {
                val (a, c) = when (s.season) {
                    Season.SPRING -> 0xFFF5A3C7.toInt() to 0xFFFFD1E3.toInt()
                    Season.AUTUMN -> 0xFFE8862E.toInt() to 0xFFC8553D.toInt()
                    Season.WINTER -> 0xFFDDE8F5.toInt() to 0xFFC9D8F0.toInt()
                    else -> l.leaf to Color.mix(l.leaf, 0xFF8BD36A.toInt(), 0.3f)
                }
                b.blit(Sprites.tree(a, 0), (w * 0.06f).toInt(), g - 21)
                b.blit(Sprites.tree(c, 1), (w * 0.86f).toInt(), g - 21)
                when (s.season) {
                    Season.AUTUMN -> b.blit(Sprites.pumpkins(), (w * 0.62f).toInt(), g - 7)
                    Season.WINTER -> b.blit(Sprites.snowdrift(), (w * 0.62f).toInt(), g - 4)
                    else -> b.blit(Sprites.bush(0xFF5DAE4F.toInt()), (w * 0.62f).toInt(), g - 6)
                }
            }
            SceneKind.FOREST -> positions.forEachIndexed { i, p ->
                b.blit(Sprites.pine(if (i % 2 == 0) l.leaf else 0xFF3A8A5A.toInt(), s.season == Season.WINTER), (w * p).toInt(), g - 23 + (i % 2) * 2)
            }
            SceneKind.SNOW -> listOf(0.05f, 0.85f, 0.66f).forEachIndexed { i, p ->
                b.blit(Sprites.pine(l.leaf, true), (w * p).toInt(), g - 23 + i)
            }
            SceneKind.DESERT -> {
                b.blit(Sprites.cactus(), (w * 0.08f).toInt(), g - 16)
                b.blit(Sprites.cactus(), (w * 0.88f).toInt(), g - 15, flip = true)
                b.blit(Sprites.rock(0xFFC08A5A.toInt()), (w * 0.62f).toInt(), g - 5)
            }
            SceneKind.SHORE -> {
                b.blit(Sprites.palm(), (w * 0.03f).toInt(), g - 24)
                b.blit(Sprites.palm(), (w * 0.88f).toInt(), g - 24, flip = true)
                // Glints on the water
                for (k in 0 until w / 12) {
                    val gx = ((k * 23 + now / 220) % w).toInt()
                    val gy = g - 7 + (k * 5) % 6
                    if ((now / 300 + k) % 3 != 0L) b.plot(gx, gy, 0xC0FFFFFF.toInt())
                }
            }
            SceneKind.CAVE -> {
                val pulse = (120 + 80 * sin(now / 700.0)).toInt()
                listOf(0.04f to 0xFF5EC8FF.toInt(), 0.62f to 0xFFE070FF.toInt(), 0.88f to 0xFF7CFFB2.toInt()).forEach { (p, c) ->
                    b.blit(Sprites.crystal(c), (w * p).toInt(), g - 14, glowAlpha = pulse)
                }
            }
        }
    }

    private fun drawGround(s: SceneState, now: Long) {
        val b = buffer
        val l = land(s.kind).let {
            if (s.season == Season.WINTER && (s.kind == SceneKind.MEADOW || s.kind == SceneKind.FOREST))
                it.copy(ground = 0xFFEEF3FB.toInt(), groundDark = 0xFFD3DEF0.toInt(), grass = 0xFFFFFFFF.toInt())
            else it
        }
        for (y in groundY until h) for (x in 0 until w) {
            val t = (y - groundY) / (h - groundY).toFloat()
            var c = Bayer.gradient(intArrayOf(l.ground, l.groundDark), t * 0.9f, x, y)
            if (((x * 7 + y * 13) % 17 == 0) && y > groundY + 1) c = l.groundDark
            b.px[y * w + x] = c
        }
        // A light lip along the edge, with tufts poking up.
        for (x in 0 until w) {
            b.px[groundY * w + x] = l.grass
            if ((x * 5 + 3) % 7 == 0 && s.kind != SceneKind.CAVE && s.kind != SceneKind.DESERT) b.plot(x, groundY - 1, l.grass)
        }
        // Little details
        val r = Random(3)
        repeat(w / 8) {
            val x = r.nextInt(w)
            val y = groundY + 2 + r.nextInt(h - groundY - 3)
            when (s.kind) {
                SceneKind.MEADOW -> if (s.season == Season.WINTER) b.plot(x, y, 0xFFFFFFFF.toInt()) else {
                    val fc = intArrayOf(0xFFFFFFFF.toInt(), 0xFFF28DB2.toInt(), 0xFFFFE066.toInt(), 0xFFB79CFF.toInt())[it % 4]
                    b.plot(x, y, fc); b.plot(x, y + 1, 0xFF3E8A40.toInt())
                }
                SceneKind.FOREST -> { b.plot(x, y, 0xFF2A5A34.toInt()); b.plot(x + 1, y, 0xFF2A5A34.toInt()) }
                SceneKind.SNOW -> b.plot(x, y, 0xFFFFFFFF.toInt())
                SceneKind.DESERT -> b.plot(x, y, 0xFFC99A5B.toInt())
                SceneKind.SHORE -> if (it % 3 == 0) { b.plot(x, y, 0xFFF28DB2.toInt()); b.plot(x + 1, y, 0xFFFFD1E0.toInt()) } else b.plot(x, y, 0xFFC9A970.toInt())
                SceneKind.CAVE -> b.plot(x, y, 0xFF6A5EA0.toInt())
            }
        }
    }

    // ---- Actors --------------------------------------------------------------------------

    private fun shadow(cx: Int, width: Int) {
        for (x in cx - width / 2..cx + width / 2) for (y in groundY + 1..groundY + 2) {
            val edge = x == cx - width / 2 || x == cx + width / 2
            if (edge && y == groundY + 2) continue
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
        val hx = heroX + lunge + hurtShake
        val hy = groundY - hero.h + 2 + bob
        // Pet
        val parts = Catalog.petParts(s.pet)
        if (parts != null) {
            val (sp, po) = parts
            val pet = Sprites.pet(sp, po, s.petGrown, blink = (now + 900) % 4100 < 140)
            val hopT = (now % 2600)
            val hop = if (hopT < 300) (sin(hopT / 300.0 * PI) * 3).roundToInt() else 0
            val px = if (s.boss != null) hx - pet.w + 3 else hx + hero.w - 2
            val py = groundY - pet.h + 2 - hop + if (sp == Species.TURTLE) 0 else 0
            shadow(px + pet.w / 2, 8)
            b.blit(pet, px, py, flip = s.boss == null, glowAlpha = if (s.petGrown) (140 + 60 * sin(now / 400.0)).toInt() else 0)
        }
        shadow(hx + hero.w / 2, 10)
        val glow = (150 + 80 * sin(now / 500.0)).toInt()
        val tint = when {
            now < hurtUntil -> 0x99E5484D.toInt()
            now < levelUntil && (now / 120) % 2 == 0L -> 0x55FFF3B0
            else -> 0
        }
        b.blit(hero, hx, hy, tint = tint, glowAlpha = glow)
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
            val by = groundY - spr.h + 3
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
            SceneKind.FOREST, SceneKind.CAVE -> 10
            SceneKind.MEADOW -> if (night > 0.5f) 9 else 4
            SceneKind.SHORE, SceneKind.DESERT -> if (night > 0.5f) 4 else 2
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
        s.season?.takeIf { it != Season.SUMMER && s.kind != SceneKind.CAVE && s.kind != SceneKind.DESERT && s.kind != SceneKind.SNOW }

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
