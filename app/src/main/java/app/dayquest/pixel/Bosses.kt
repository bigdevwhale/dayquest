package app.dayquest.pixel

import app.dayquest.game.BossShape
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The bosses added after the first ten; [Sprites.boss] hands them over here. */
internal object MoreBosses {
    private val EYE = Flat(0xFF2A1F38.toInt())
    private val WHITE = Flat(0xFFFFFFFF.toInt())
    private fun mat(c: Int, glows: Boolean = false) = Mat(Ramps.of(c), glows)
    private fun tone(c: Int, i: Int) = Tone(Ramps.of(c), i)

    fun draw(shape: BossShape, primary: Int, secondary: Int, frame: Int): Sprite {
        val p = mat(primary)
        val s = mat(secondary)
        val glowS = mat(secondary, glows = true)
        return when (shape) {
            BossShape.SPIDER -> PixelCanvas(36, 26).apply {
                val leg = mat(Color.mix(primary, 0xFF1A1428.toInt(), 0.35f))
                val f = frame
                // Four legs a side, each with a high knee
                listOf(-1 to 0, -1 to 1, -1 to 2, -1 to 3, 1 to 0, 1 to 1, 1 to 2, 1 to 3).forEach { (side, k) ->
                    val bx = 18 + side * 4
                    val by = 12 + k
                    val kx = 18 + side * (9 + k * 2)
                    val ky = 4 + k * 2 + if ((k + f) % 2 == 0) 1 else 0
                    val fx = 18 + side * (12 + k * 3 - (k / 2) * 2)
                    line(bx, by, kx, ky, leg, 1); line(kx, ky, fx.coerceIn(0, 35), 25, leg, 1)
                }
                ellipse(18f, 10f, 9f, 7.5f, p)
                grid(listOf("s.....s", ".s...s.", "..sss..", ".s...s.", "s.....s"), mapOf('s' to s), dx = 15, dy = 6)
                ellipse(18f, 18.5f, 5.5f, 4.5f, p)
                listOf(15 to 17, 17 to 16, 19 to 16, 21 to 17, 16 to 19, 20 to 19).forEach { (x, y) -> set(x, y, glowS) }
                set(16, 22, WHITE); set(20, 22, WHITE); set(16, 23, WHITE); set(20, 23, WHITE)
                // A thread up to the ceiling
                line(18, 0, 18, 2, Flat(0xC0FFFFFF.toInt()))
            }.render()
            BossShape.KRAKEN -> PixelCanvas(36, 32).apply {
                val sucker = s
                // Tentacles curling out under the head
                for (k in 0 until 6) {
                    val side = if (k < 3) -1 else 1
                    val i = k % 3
                    var x = 18f + side * (2f + i * 2.2f)
                    var y = 16f
                    var a = Math.PI / 2 - side * (0.35 + i * 0.35)
                    for (step in 0 until 15) {
                        val th = if (step < 7) 2 else 1
                        ellipse(x, y, th.toFloat(), th.toFloat(), p)
                        if (step % 3 == 1 && step < 12) set(x.roundToInt(), y.roundToInt() + 1, sucker)
                        a += side * (-0.06 + 0.04 * step) * (if (frame == 1) 1.25 else 1.0)
                        x += cos(a).toFloat() * 1.2f
                        y += sin(a).toFloat() * 1.0f
                    }
                }
                ellipse(18f, 10f, 10f, 9.5f, p)
                ellipse(18f, 14.5f, 8f, 3.5f, p)
                // Big eyes and a frown
                listOf(13f, 23f).forEach { ex ->
                    ellipse(ex, 11f, 3f, 3.2f, mat(0xFFF6EBDD.toInt()))
                    rect(ex.toInt() - 1, 10, ex.toInt(), 12, mat(secondary, glows = true)); set(ex.toInt() - 1, 10, EYE)
                }
                line(10, 7, 15, 9, EYE); line(26, 7, 21, 9, EYE)
                listOf(9 to 4, 26 to 3, 17 to 2, 22 to 6).forEach { (x, y) -> set(x, y, tone(primary, 1)) }
            }.render()
            BossShape.MIMIC -> PixelCanvas(32, 28).apply {
                val wood = p
                val trim = mat(secondary)
                val open = if (frame == 1) 2 else 0
                // Lid thrown back
                tri(2f, 9f - open, 30f, 9f - open, 27f, 1f, wood); tri(2f, 9f - open, 5f, 1f, 27f, 1f, wood)
                rect(2, 7 - open, 29, 8 - open, trim); rect(14, 1, 17, 8 - open, trim)
                // Box with a toothy mouth
                rect(2, 13, 29, 26, wood)
                rect(3, 9 - open, 28, 15, Flat(0xFF1A1020.toInt()))
                for (x in 3..27 step 3) { set(x, 10 - open, WHITE); set(x + 1, 10 - open, WHITE); set(x + 1, 11 - open, WHITE) }
                for (x in 4..27 step 3) { set(x, 15, WHITE); set(x + 1, 14, WHITE); set(x, 14, WHITE) }
                ellipse(18f, 15f, 3f, 1.6f, mat(0xFFE36B8A.toInt())); rect(19, 15, 21, 18, mat(0xFFE36B8A.toInt()))
                set(10, 12 - open / 2, mat(0xFFFFE066.toInt(), glows = true)); set(21, 12 - open / 2, mat(0xFFFFE066.toInt(), glows = true))
                rect(2, 16, 29, 16, trim); rect(2, 25, 29, 26, trim); rect(2, 16, 3, 26, trim); rect(28, 16, 29, 26, trim)
                rect(14, 18, 17, 22, trim); set(15, 20, EYE); set(16, 20, EYE)
                // Coins spilling out
                listOf(0 to 26, 31 to 25, 30 to 27).forEach { (x, y) -> set(x, y, mat(0xFFF2C14E.toInt(), glows = true)) }
            }.render()
            BossShape.GHOST -> PixelCanvas(30, 32).apply {
                val sheet = mat(primary, glows = true)
                ellipse(15f, 11f, 10f, 10f, sheet)
                rect(5, 11, 25, 26, sheet)
                // Ragged hem that ripples
                for (x in 5..25) {
                    val wave = ((x + frame * 2) / 3) % 2
                    for (y in 26..27 + wave * 2) set(x, y, sheet)
                    if (wave == 0 && x % 3 == 0) set(x, 27, null)
                }
                // Arms reaching out
                ellipse(3.5f, 17f + frame, 2.6f, 2f, sheet); ellipse(26.5f, 16f - frame, 2.6f, 2f, sheet)
                // Hollow eyes with a glint and an O of a mouth
                ellipse(11f, 11f, 2.4f, 3.2f, Flat(0xFF1E1530.toInt())); ellipse(19f, 11f, 2.4f, 3.2f, Flat(0xFF1E1530.toInt()))
                set(11, 11, mat(secondary, glows = true)); set(19, 11, mat(secondary, glows = true))
                ellipse(15f, 18f, 1.8f, 2.2f, Flat(0xFF1E1530.toInt()))
                // A broken chain
                val chain = mat(0xFF8E99A6.toInt())
                listOf(22 to 22, 24 to 23, 26 to 24, 28 to 26).forEach { (x, y) -> set(x, y, chain); set(x + 1, y, chain) }
            }.render()
            BossShape.KNIGHT -> PixelCanvas(30, 34).apply {
                val steel = p
                val cape = mat(Color.mix(secondary, 0xFF2A1020.toInt(), 0.35f))
                // Cape behind
                tri(15f, 9f, 3f, 33f, 27f, 33f, cape)
                // Legs and body
                rect(9, 26, 13, 33, steel); rect(17, 26, 21, 33, steel)
                rect(8, 14, 22, 26, steel)
                ellipse(6f, 15f, 4f, 3f, steel); ellipse(24f, 15f, 4f, 3f, steel)
                rect(4, 17, 7, 24, steel); rect(23, 17, 26, 24, steel)
                rect(8, 24, 22, 25, mat(0xFF5A3A24.toInt())); rect(14, 24, 16, 25, mat(0xFFF2C14E.toInt()))
                line(15, 15, 15, 22, tone(primary, 1))
                // Great helm with a burning visor and a plume
                ellipse(15f, 8f, 5.5f, 6f, steel)
                rect(11, 7, 19, 8, Flat(0xFF120C18.toInt()))
                rect(12, 7, 13, 7, glowS); rect(17, 7, 18, 7, glowS)
                val plume = mat(secondary)
                ellipse(15f, 1.5f, 2f, 2f, plume); ellipse(18f, 1f + frame, 2.5f, 1.6f, plume)
                // A huge sword planted in front
                val blade = mat(0xFFD6DEE8.toInt())
                rect(26, 6, 27, 30, blade); rect(23, 25, 30, 26, mat(0xFFF2C14E.toInt())); rect(26, 27, 27, 33, mat(0xFF4A3424.toInt()))
                set(26, 5, blade)
            }.render()
            BossShape.HYDRA -> PixelCanvas(38, 32).apply {
                val body = p
                val belly = mat(secondary)
                fun neck(x0: Float, y0: Float, x1: Float, y1: Float) {
                    for (k in 0..10) {
                        val t = k / 10f
                        val bend = sin(t * 3.1f) * 2f
                        ellipse(x0 + (x1 - x0) * t + bend, y0 + (y1 - y0) * t, 2.4f, 2.4f, body)
                    }
                }
                fun head(x: Float, y: Float, flip: Boolean) {
                    val d = if (flip) -1 else 1
                    ellipse(x, y, 3.6f, 2.8f, body)
                    tri(x + d * 2f, y - 1.5f, x + d * 6.5f, y + 0.5f, x + d * 2f, y + 2.5f, body)
                    tri(x - d * 1f, y - 2f, x - d * 3.5f, y - 6f, x + d * 0.5f, y - 2f, belly)
                    set((x + d * 0.5f).toInt(), (y - 0.5f).toInt(), mat(0xFFFFE066.toInt(), glows = true))
                    set((x + d * 5f).toInt(), (y + 1.5f).toInt(), WHITE)
                }
                val b = frame.toFloat()
                neck(13f, 20f, 6f, 9f + b); neck(19f, 18f, 19f, 6f - b); neck(25f, 20f, 31f, 10f + b)
                ellipse(19f, 24f, 11f, 7f, body)
                ellipse(19f, 26f, 6f, 4f, belly)
                rect(11, 28, 14, 31, body); rect(24, 28, 27, 31, body)
                head(6f, 8f + b, true); head(19f, 4.5f - b, false); head(31f, 9f + b, false)
                listOf(12 to 21, 26 to 22, 19 to 19).forEach { (x, y) -> set(x, y, tone(primary, 1)) }
            }.render()
            BossShape.BEE -> PixelCanvas(32, 28).apply {
                val wing = Mat(Ramps.of(0xFFBFE6FF.toInt()), glows = true)
                val up = frame == 1
                ellipse(13f, if (up) 4f else 6f, 4.5f, 3f, wing); ellipse(20f, if (up) 3f else 5f, 5f, 3.4f, wing)
                set(12, if (up) 3 else 5, WHITE); set(19, if (up) 2 else 4, WHITE)
                // Striped abdomen, thorax, head
                ellipse(20f, 18f, 9f, 7f, p)
                listOf(17, 21, 25).forEach { x -> for (y in 10..26) if (this[x, y] === p) { set(x, y, tone(0xFF2E2433.toInt(), 2)); set(x + 1, y, tone(0xFF2E2433.toInt(), 2)) } }
                tri(28f, 17f, 32f, 19f, 28f, 21f, mat(0xFF2E2433.toInt()))
                ellipse(10f, 16f, 5f, 4.5f, mat(0xFF2E2433.toInt()))
                ellipse(6f, 13f, 4.5f, 4f, p)
                set(5, 12, EYE); set(4, 12, EYE); set(5, 11, WHITE)
                line(4, 9, 2, 4, mat(0xFF2E2433.toInt())); line(7, 9, 8, 4, mat(0xFF2E2433.toInt()))
                set(2, 3, s); set(8, 3, s)
                // A tiny crown and legs
                grid(listOf("g.g.g", "ggggg"), mapOf('g' to mat(secondary, glows = true)), dx = 4, dy = 6)
                listOf(9, 12, 15).forEach { x -> line(x, 20, x - 1, 24, mat(0xFF2E2433.toInt())) }
            }.render()
            BossShape.MUSHROOM -> PixelCanvas(32, 32).apply {
                val stem = mat(secondary)
                rect(10, 15, 21, 30, stem); ellipse(15.5f, 29f, 7.5f, 2.5f, stem)
                // Arms
                line(10, 19, 4, 23 - frame, stem, 2); line(21, 19, 27, 23 - frame, stem, 2)
                // The cap
                ellipse(16f, 10f, 15f, 9f, p)
                rect(1, 13, 30, 15, null)
                rect(2, 13, 29, 14, tone(primary, 1))
                listOf(Triple(8f, 6f, 2.2f), Triple(17f, 4f, 2.6f), Triple(24f, 8f, 1.8f), Triple(12f, 10f, 1.5f), Triple(27f, 12f, 1.2f)).forEach { (x, y, r) -> ellipse(x, y, r, r, mat(0xFFF6EBDD.toInt())) }
                // Grumpy face
                rect(12, 18, 13, 19, EYE); rect(18, 18, 19, 19, EYE); set(12, 18, WHITE); set(18, 18, WHITE)
                line(11, 16, 14, 17, EYE); line(20, 16, 17, 17, EYE)
                rect(14, 22, 17, 22, EYE)
                // Spores
                listOf(3 to 2, 29 to 3, 1 to 9, 31 to 18).forEach { (x, y) -> set(x, y - frame, mat(0xFFB7FF7A.toInt(), glows = true)) }
            }.render()
            BossShape.JELLYFISH -> PixelCanvas(30, 34).apply {
                val bell = mat(primary, glows = true)
                val tent = mat(secondary, glows = true)
                ellipse(15f, 12f, 12.5f, 11f, bell)
                rect(1, 14, 29, 24, null)
                for (x in 3..27) { set(x, 13, bell); if ((x + frame) % 3 != 0) set(x, 14, bell) }
                for (k in 0 until 6) {
                    val x0 = 7 + k * 3
                    for (y in 12..32) {
                        val x = x0 + (sin((y + frame * 3 + k) / 2.5) * 1.3).roundToInt()
                        if (y < 33 - (k % 3) * 3) set(x, y, tent)
                    }
                }
                set(11, 8, EYE); set(19, 8, EYE); set(11, 7, WHITE); set(19, 7, WHITE)
                set(9, 10, Flat(0xFFF28DB2.toInt())); set(21, 10, Flat(0xFFF28DB2.toInt()))
                line(14, 10, 16, 10, EYE)
                listOf(8 to 5, 21 to 4, 15 to 2).forEach { (x, y) -> set(x, y, WHITE) }
            }.render()
            BossShape.SCORPION -> PixelCanvas(38, 28).apply {
                val shell = p
                // Tail arching over the back, stinger glowing
                val tail = listOf(26f to 20f, 29f to 17f, 31f to 13f, 31f to 9f, 29f to 5.5f, 25.5f to 4f)
                tail.forEachIndexed { k, (x, y) -> ellipse(x, y + if (k > 3) frame.toFloat() else 0f, 2.8f - k * 0.2f, 2.6f - k * 0.2f, shell) }
                tri(24f, 3f + frame, 20f, 5f + frame, 23f, 7f + frame, glowS)
                // Legs
                listOf(12, 16, 20, 24).forEach { x -> line(x, 21, x - 3, 26, shell); line(x + 1, 21, x + 3, 26, shell) }
                ellipse(18f, 19f, 10f, 4.5f, shell)
                listOf(14, 18, 22).forEach { x -> for (y in 15..23) if (this[x, y] === shell) set(x, y, tone(primary, 1)) }
                ellipse(8f, 19f, 4f, 3.5f, shell)
                set(6, 17, mat(secondary, glows = true)); set(8, 17, mat(secondary, glows = true))
                // Pincers
                val lift = frame.toFloat()
                line(6, 20, 2, 15, shell, 2)
                ellipse(3f, 12f - lift, 3f, 2.5f, shell); tri(0f, 9f - lift, 3f, 11f - lift, 0f, 12f - lift, null)
                line(9, 22, 6, 25, shell, 2)
                ellipse(4f, 24f, 3f, 2.2f, shell)
            }.render()
            BossShape.SCARECROW -> PixelCanvas(32, 34).apply {
                val pole = mat(0xFF7A5034.toInt())
                val straw = mat(0xFFE8C15A.toInt())
                val coat = p
                rect(15, 12, 16, 33, pole)
                rect(2, 14, 29, 15, pole)
                // Coat with a patch, straw hands
                tri(15.5f, 12f, 6f, 28f, 25f, 28f, coat)
                rect(7, 14, 24, 20, coat)
                rect(18, 21, 21, 24, mat(Color.mix(secondary, 0xFFE8C15A.toInt(), 0.3f)))
                listOf(1 to 13, 0 to 15, 1 to 17, 30 to 13, 31 to 15, 30 to 17).forEach { (x, y) -> line(x, y, x + if (x < 10) 3 else -3, 15, straw) }
                listOf(9, 13, 18, 22).forEach { x -> line(x, 28, x - 1, 31, straw) }
                // Sack head with a stitched grin, witch hat
                ellipse(15.5f, 9f, 5.5f, 5f, mat(0xFFD9B98A.toInt()))
                set(13, 8, mat(0xFFFF7A3D.toInt(), glows = true)); set(18, 8, mat(0xFFFF7A3D.toInt(), glows = true))
                line(12, 11, 19, 11, EYE); listOf(13, 15, 17).forEach { x -> set(x, 12, EYE) }
                val hat = s
                rect(8, 4, 23, 4, hat); tri(11f, 4f, 20f, 4f, 17f, -3f, hat)
                // A crow perched on the arm
                val crow = mat(0xFF2E2433.toInt())
                ellipse(26f, 11f - frame, 2.2f, 2f, crow); set(28, 10 - frame, mat(0xFFF2C14E.toInt())); set(25, 10 - frame, Flat(0xFFE5484D.toInt()))
            }.render()
            BossShape.BAT -> PixelCanvas(36, 26).apply {
                val wing = mat(Color.mix(primary, 0xFFB79CFF.toInt(), 0.3f))
                val up = frame == 1
                // Webbed wings: a fan of fingers with scalloped skin between them
                val bone = mat(Color.mix(primary, 0xFF1A1428.toInt(), 0.5f))
                val tips = if (up) listOf(1f to 2f, 0f to 10f, 3f to 16f, 8f to 18f) else listOf(1f to 9f, 1f to 16f, 5f to 21f, 10f to 21f)
                for (side in listOf(-1, 1)) {
                    val sx = 18f + side * 5f
                    val sy = 11f
                    val pts = tips.map { (x, y) -> (if (side < 0) x else 36f - x) to y }
                    for (k in 0 until pts.size - 1) {
                        tri(sx, sy, pts[k].first, pts[k].second, pts[k + 1].first, pts[k + 1].second, wing)
                        val mx = (pts[k].first + pts[k + 1].first) / 2 + side * 0.5f
                        val my = (pts[k].second + pts[k + 1].second) / 2 + 1.2f
                        ellipse(mx - side * 1f, my, 2f, 1.8f, null)
                    }
                    pts.forEach { (x, y) -> line(sx.toInt(), sy.toInt(), x.toInt(), y.toInt(), bone) }
                    tri(sx, sy - 1, pts[0].first, pts[0].second - 1, sx - side * 3, sy - 3, bone)
                }
                ellipse(18f, 15f, 6f, 7f, p)
                ellipse(18f, 17f, 3.5f, 4f, s)
                tri(13f, 9f, 14f, 1f, 17f, 8f, p); tri(23f, 9f, 22f, 1f, 19f, 8f, p)
                ellipse(18f, 10f, 5f, 4f, p)
                set(16, 10, mat(0xFFFF4A5A.toInt(), glows = true)); set(20, 10, mat(0xFFFF4A5A.toInt(), glows = true))
                set(17, 13, WHITE); set(19, 13, WHITE)
                set(16, 22, p); set(20, 22, p)
            }.render()
            BossShape.SNOWMAN -> PixelCanvas(30, 34).apply {
                val snow = p
                ellipse(15f, 26f, 10f, 7.5f, snow)
                ellipse(15f, 16f, 7.5f, 6f, snow)
                ellipse(15f, 8f, 5.5f, 5f, snow)
                // Coal eyes, carrot nose, buttons
                set(13, 7, EYE); set(17, 7, EYE); set(13, 6, WHITE)
                rect(15, 9, 19, 9, mat(0xFFF08A2E.toInt())); set(15, 8, mat(0xFFF08A2E.toInt()))
                listOf(11, 13, 15, 17).forEach { x -> set(x, 11 + if (x == 11 || x == 17) 0 else 1, EYE) }
                listOf(16, 19, 24).forEach { y -> set(15, y, EYE) }
                // Scarf and hat
                val scarf = s
                rect(9, 12, 21, 13, scarf); rect(18, 13, 20, 18, scarf)
                val hat = mat(0xFF2E2433.toInt())
                rect(8, 3, 22, 3, hat); rect(10, -2, 20, 2, hat); rect(10, 1, 20, 1, scarf)
                // Twig arms waving
                val twig = mat(0xFF6B4630.toInt())
                line(8, 16, 1, 10 + frame * 2, twig); line(2, 11 + frame * 2, 0, 9 + frame * 2, twig)
                line(22, 16, 29, 12 - frame, twig); line(28, 12 - frame, 29, 9 - frame, twig)
            }.render()
            BossShape.PENGUIN -> PixelCanvas(28, 32).apply {
                val body = p
                ellipse(14f, 18f, 10f, 12.5f, body)
                ellipse(14f, 20f, 6.5f, 9.5f, mat(0xFFF6F2EA.toInt()))
                ellipse(14f, 8f, 6f, 5.5f, body)
                ellipse(10.5f, 9f, 2f, 2f, mat(0xFFF6F2EA.toInt())); ellipse(17.5f, 9f, 2f, 2f, mat(0xFFF6F2EA.toInt()))
                set(11, 9, EYE); set(17, 9, EYE)
                tri(12f, 11f, 16f, 11f, 14f, 14.5f, mat(0xFFF2A03D.toInt()))
                // Flippers, one raised; feet
                ellipse(3.5f, 18f - frame * 2, 2.5f, 6f, body); ellipse(24.5f, 19f, 2.5f, 6f, body)
                rect(8, 30, 12, 31, mat(0xFFF2A03D.toInt())); rect(16, 30, 20, 31, mat(0xFFF2A03D.toInt()))
                // Emperor's crown and a dash of gold on the chest
                grid(listOf("g.g.g", "ggggg", "grgrg"), mapOf('g' to mat(secondary, glows = true), 'r' to Flat(0xFF5EC8FF.toInt())), dx = 12, dy = 0)
                ellipse(14f, 15f, 2.5f, 1.5f, mat(0xFFF2C14E.toInt()))
            }.render()
            else -> PixelCanvas(4, 4).render()
        }
    }
}
