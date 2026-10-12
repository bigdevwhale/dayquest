package app.dayquest.pixel

import app.dayquest.game.Spot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** How far below the horizon the garden path runs; the hero stands on it. */
const val GARDEN_PATH = 16

/** The corner of the hero's cottage at the garden's left edge; its window glows at night. */
private fun cottage(lit: Boolean): Sprite = Sprites.cached("cottage:$lit") {
    val c = PixelCanvas(38, 46)
    val plaster = Ramps.of(0xFFEDE2CF.toInt())
    val beam = Ramps.of(0xFF6B4630.toInt())
    for (y in 14..45) for (x in 0..33) c.set(x, y, Tone(plaster, if (x > 30) 1 else if (noise(x, y) > 0.92f) 1 else 2))
    // Timber frame
    listOf(0, 16, 32).forEach { x -> c.rect(x, 14, x + 1, 45, Tone(beam, if (x == 0) 3 else 2)) }
    c.rect(0, 14, 33, 15, Tone(beam, 2)); c.rect(0, 29, 33, 30, Tone(beam, 2))
    c.line(2, 28, 15, 16, Tone(beam, 2)); c.line(18, 16, 31, 28, Tone(beam, 1))
    // Roof hanging over the wall
    val roof = Ramps.of(0xFFC8553D.toInt())
    for (y in 0..14) for (x in 0..37) {
        val edge = 37 - y * 2
        if (x <= edge) c.set(x, y, Tone(roof, if (y % 3 == 2) 1 else if (x > edge - 3) 3 else 2))
    }
    c.rect(0, 13, 37, 14, Tone(roof, 1))
    // Chimney
    c.rect(6, 0, 10, 4, Tone(Ramps.of(0xFF9AA0B5.toInt()), 2)); c.rect(6, 0, 10, 0, Tone(Ramps.of(0xFF9AA0B5.toInt()), 3))
    // Door with a lantern
    val door = Ramps.of(0xFF8A5A36.toInt())
    c.rect(4, 33, 12, 45, Tone(door, 2)); c.rect(4, 33, 4, 45, Tone(door, 3)); c.rect(8, 33, 8, 45, Tone(door, 1)); c.ellipse(8.5f, 33f, 4.5f, 2f, Tone(door, 2))
    c.set(11, 39, Mat(Ramps.of(0xFFF2C14E.toInt())))
    c.rect(14, 33, 15, 35, if (lit) Mat(Ramps.of(0xFFFFE066.toInt()), glows = true) else Tone(Ramps.of(0xFF3A3550.toInt()), 2))
    // Window and flower box
    val glass = if (lit) Mat(Ramps.of(0xFFFFD27A.toInt()), glows = true) else Flat(0xFF8CC8F5.toInt())
    c.rect(20, 18, 29, 26, Tone(beam, 2)); c.rect(21, 19, 28, 25, glass); c.rect(24, 19, 25, 25, Tone(beam, 2)); c.rect(21, 22, 28, 22, Tone(beam, 2))
    if (!lit) { c.set(22, 20, Flat(0xFFFFFFFF.toInt())); c.set(23, 20, Flat(0xFFD4ECFA.toInt())) }
    c.rect(19, 27, 30, 28, Tone(Ramps.of(0xFFB07A45.toInt()), 2))
    listOf(20, 23, 26, 29).forEachIndexed { i, x -> c.set(x, 26, Mat(Ramps.of(intArrayOf(0xFFE2435F.toInt(), 0xFFFFE066.toInt(), 0xFFF28DB2.toInt(), 0xFFB79CFF.toInt())[i]))) }
    c.render()
}

private fun SceneRenderer.spotAnchor(spot: Spot): Pair<Int, Int> = when (spot) {
    Spot.G_BIG_L -> (w * 0.37f).roundToInt() to groundY + 9
    Spot.G_BIG_R -> (w * 0.86f).roundToInt() to groundY + 9
    Spot.G_SMALL_L -> (w * 0.27f).roundToInt() to groundY + 15
    Spot.G_SMALL_C -> (w * 0.69f).roundToInt() to groundY + 14
    Spot.G_SMALL_R -> (w * 0.95f).roundToInt() to h - 2
    else -> w / 2 to groundY
}

/** Fence along the back, the big pieces, the path, the cottage and the small things in front. */
internal fun SceneRenderer.drawGarden(placement: Map<Spot, String>, now: Long, night: Float) {
    val b = buffer
    val frame = ((now / 450) % 2).toInt()
    val house = cottage(night > 0.45f)
    val houseW = house.w - 4
    placement[Spot.G_FENCE]?.let { key ->
        val tile = Sprites.decor(key)
        var x = houseW - 2
        while (x < w) { b.blit(tile, x, groundY + 4 - tile.h); x += tile.w - 2 }
    }
    for (spot in listOf(Spot.G_BIG_L, Spot.G_BIG_R)) {
        val key = placement[spot] ?: continue
        val spr = Sprites.decor(key, frame)
        val (cx, by) = spotAnchor(spot)
        val bottom = if (key == "pond") by + 5 else by
        b.blit(spr, cx - spr.w / 2, bottom - spr.h, glowAlpha = 160)
    }
    placement[Spot.G_PATH]?.let { key ->
        val tile = Sprites.decor(key)
        var x = 10
        while (x < w) { b.blit(tile, x, groundY + GARDEN_PATH); x += tile.w - 2 }
    }
    b.blit(house, -2, groundY + 11 - house.h, glowAlpha = if (night > 0.45f) 200 else 0)
    for (spot in listOf(Spot.G_SMALL_L, Spot.G_SMALL_C, Spot.G_SMALL_R)) {
        val key = placement[spot] ?: continue
        val spr = Sprites.decor(key, frame)
        val (cx, by) = spotAnchor(spot)
        b.blit(spr, cx - spr.w / 2, by - spr.h, glowAlpha = 160)
    }
}

/** Warm pools of light after dark: the cottage window, lamp posts and lanterns. */
internal fun SceneRenderer.gardenLights(placement: Map<Spot, String>, night: Float) {
    val lights = ArrayList<Triple<Int, Int, Int>>()
    lights += Triple(24, groundY - 13, 22)
    for (spot in listOf(Spot.G_SMALL_L, Spot.G_SMALL_C, Spot.G_SMALL_R)) {
        if (placement[spot] != "lamp_post") continue
        val (cx, by) = spotAnchor(spot)
        lights += Triple(cx, by - 23, 30)
    }
    val warm = 0xFFFFC27A.toInt()
    for ((cx, cy, r) in lights) for (y in cy - r..cy + r) for (x in cx - r..cx + r) {
        if (x !in 0 until w || y !in 0 until h) continue
        val d = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toFloat()) / r
        if (d >= 1f) continue
        val a = (1f - d) * (1f - d) * 0.45f * night
        if (a < Bayer.at(x, y) * 0.08f) continue
        val i = y * w + x
        val c = buffer.px[i]
        val lit = Color.argb(255, (Color.r(c) * 1.4f).toInt().coerceAtMost(255), (Color.g(c) * 1.3f).toInt().coerceAtMost(255), (Color.b(c) * 1.1f).toInt().coerceAtMost(255))
        buffer.px[i] = Color.mix(c, Color.mix(lit, warm, 0.35f), a)
    }
}
