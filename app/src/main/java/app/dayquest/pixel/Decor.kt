package app.dayquest.pixel

/** Furniture and garden pieces added after the first set; [Sprites.decor] asks here for the rest. */
internal object MoreDecor {
    private val EYE = Flat(0xFF2A1F38.toInt())
    private val WHITE = Flat(0xFFFFFFFF.toInt())
    private fun mat(c: Int, glows: Boolean = false) = Mat(Ramps.of(c), glows)
    private fun tone(c: Int, i: Int) = Tone(Ramps.of(c), i)
    private val WOOD = 0xFF9A6334.toInt()
    private val DARK = 0xFF6B4630.toInt()
    private val GOLD = 0xFFF2C14E.toInt()
    private val STONE = 0xFFA9AFC2.toInt()

    val animated = setOf("fountain", "windmill", "beehive", "cauldron", "crystal_ball", "swing", "pond", "lamp_post")

    fun canvas(key: String, frame: Int): PixelCanvas? = when (key) {
        // ---- Room: wall ----
        "plant_shelf" -> PixelCanvas(20, 14).apply {
            rect(0, 11, 19, 12, mat(WOOD)); set(2, 13, mat(DARK)); set(17, 13, mat(DARK))
            rect(2, 7, 6, 10, mat(0xFFC8693D.toInt())); rect(13, 8, 17, 10, mat(0xFF5EC8FF.toInt()))
            val leaf = Ramps.of(0xFF4FAE5A.toInt())
            ball(4f, 4.5f, 3f, 3f, leaf); line(4, 6, 1, 10, Tone(leaf, 2)); line(5, 7, 7, 11, Tone(leaf, 2))
            ball(15f, 5.5f, 2.6f, 3f, Ramps.of(0xFF6DBA5A.toInt())); set(15, 3, Flat(0xFFF28DB2.toInt())); set(14, 5, Flat(0xFFF28DB2.toInt()))
            rect(9, 9, 10, 10, mat(0xFFE2435F.toInt())); rect(9, 6, 10, 8, mat(0xFF4F7BD9.toInt()))
        }
        "weapon_rack" -> PixelCanvas(24, 18).apply {
            val sh = Ramps.of(0xFF3D6FD9.toInt())
            line(2, 2, 21, 16, mat(0xFFD6DEE8.toInt()), 1); line(21, 2, 2, 16, mat(0xFFD6DEE8.toInt()), 1)
            rect(4, 13, 6, 14, mat(DARK)); rect(17, 13, 19, 14, mat(DARK))
            for (y in 3..14) for (x in 7..16) {
                val f = (y - 3) / 11f
                if (x in (7 + (f * f * 4).toInt())..(16 - (f * f * 4).toInt())) set(x, y, Tone(sh, if (x < 10) 3 else if (x > 14) 1 else 2))
            }
            rect(11, 4, 12, 13, tone(GOLD, 3)); rect(8, 7, 15, 8, tone(GOLD, 3))
        }
        // ---- Room: big ----
        "sofa" -> PixelCanvas(30, 16).apply {
            val fab = mat(0xFF4F9BEF.toInt())
            rect(2, 2, 27, 10, fab); ellipse(3f, 9f, 3f, 4f, fab); ellipse(26f, 9f, 3f, 4f, fab)
            rect(3, 9, 26, 12, mat(0xFF6FB6F2.toInt()))
            line(15, 3, 15, 12, tone(0xFF4F9BEF.toInt(), 1))
            ellipse(8f, 7f, 3f, 2.2f, mat(0xFFF2C14E.toInt())); ellipse(22f, 7f, 3f, 2.2f, mat(0xFFF28DB2.toInt()))
            rect(2, 13, 27, 13, mat(DARK)); rect(3, 14, 4, 15, mat(DARK)); rect(25, 14, 26, 15, mat(DARK))
        }
        "armor_stand" -> PixelCanvas(18, 30).apply {
            val steel = mat(0xFFC9D2DC.toInt())
            rect(8, 22, 9, 28, mat(DARK)); rect(4, 28, 13, 29, mat(DARK))
            ellipse(9f, 4.5f, 4f, 4.5f, steel); rect(6, 4, 11, 4, Flat(0xFF2A2236.toInt())); set(9, 0, mat(0xFFE2435F.toInt()))
            rect(5, 9, 12, 20, steel); ellipse(4f, 10f, 2.5f, 2f, steel); ellipse(13f, 10f, 2.5f, 2f, steel)
            line(9, 10, 9, 19, tone(0xFFC9D2DC.toInt(), 1)); rect(5, 18, 12, 18, mat(0xFF5A3A24.toInt())); set(9, 18, mat(GOLD))
            rect(14, 3, 14, 24, mat(0xFFD6DEE8.toInt())); rect(13, 20, 15, 20, mat(GOLD))
        }
        "alchemy_table" -> PixelCanvas(30, 22).apply {
            rect(0, 10, 29, 12, mat(WOOD)); rect(2, 13, 4, 21, mat(WOOD)); rect(25, 13, 27, 21, mat(WOOD)); rect(5, 18, 24, 18, mat(DARK))
            val glass = mat(0xFFCFD8EA.toInt())
            ellipse(6f, 7f, 3f, 3f, glass); rect(5, 2, 6, 4, glass); ellipse(6f, 7.5f, 2.2f, 1.8f, mat(0xFF7CFFB2.toInt(), glows = true))
            rect(12, 4, 15, 9, glass); rect(12, 6, 15, 9, mat(0xFFE070FF.toInt(), glows = true))
            ellipse(22f, 8f, 3f, 2f, glass); rect(21, 3, 22, 6, glass); rect(20, 8, 24, 9, mat(0xFFFF7A3D.toInt(), glows = true))
            set(13, 2 - frame, mat(0xFFE070FF.toInt(), glows = true)); set(6, 0 + frame, mat(0xFF7CFFB2.toInt(), glows = true))
            rect(17, 8, 19, 9, mat(0xFFF1DDB0.toInt()))
        }
        // ---- Room: small ----
        "books" -> PixelCanvas(12, 10).apply {
            listOf(0xFFE2435F, 0xFF4F7BD9, 0xFF4FC07A, 0xFFF2C14E).forEachIndexed { i, c -> rect(1 + (i % 2), 8 - i * 2, 10 - (i % 2), 9 - i * 2, mat(c.toInt())) }
            rect(9, 1, 10, 1, mat(0xFFF1DDB0.toInt()))
        }
        "cauldron" -> PixelCanvas(14, 13).apply {
            val iron = mat(0xFF3A3550.toInt())
            ellipse(7f, 7.5f, 6.5f, 5f, iron); rect(1, 4, 12, 5, iron)
            rect(2, 4, 11, 4, mat(0xFF7CFFB2.toInt(), glows = true))
            rect(2, 12, 3, 12, iron); rect(10, 12, 11, 12, iron)
            set(4 + frame * 3, 2, mat(0xFF7CFFB2.toInt(), glows = true)); set(9 - frame * 2, 1 + frame, mat(0xFFB7FF7A.toInt(), glows = true))
        }
        "crystal_ball" -> PixelCanvas(12, 14).apply {
            ellipse(6f, 5.5f, 5f, 5f, mat(0xFFB79CFF.toInt(), glows = true))
            set(4, 3, WHITE); set(3, 4, WHITE)
            val sw = if (frame == 0) listOf(6 to 5, 7 to 6, 5 to 7) else listOf(5 to 5, 6 to 7, 7 to 4)
            sw.forEach { (x, y) -> set(x, y, Flat(0xFFF6EBFF.toInt())) }
            rect(2, 10, 9, 11, mat(GOLD)); rect(1, 12, 10, 13, mat(DARK))
        }
        "rug_fur" -> PixelCanvas(44, 9).apply {
            ellipse(22f, 4.5f, 21f, 4.3f, Flat(0xFFE9E1D4.toInt()))
            for (x in 1..42 step 3) set(x, if (x % 2 == 0) 0 else 8, Flat(0xFFD5CCBD.toInt()))
            ellipse(22f, 4.5f, 15f, 2.6f, Flat(0xFFF6F1E8.toInt()))
        }
        "rug_star" -> PixelCanvas(54, 9).apply {
            rect(0, 0, 53, 8, Flat(0xFF1B1F4A.toInt())); rect(1, 1, 52, 7, Flat(0xFF2B2F6A.toInt()))
            for (x in 4..50 step 7) { set(x, 2 + (x % 3), Flat(0xFFFFE17A.toInt())); set(x + 3, 6 - (x % 2), Flat(0xFFF6EBDD.toInt())) }
            ellipse(27f, 4.5f, 2.5f, 2.5f, Flat(0xFFF2C14E.toInt()))
        }
        // ---- Garden: big ----
        "apple_tree" -> PixelCanvas(28, 34).apply {
            val bark = Ramps.of(0xFF7A5034.toInt())
            rect(12, 18, 15, 33, Tone(bark, 2)); rect(12, 18, 12, 33, Tone(bark, 3)); rect(15, 20, 15, 33, Tone(bark, 1))
            set(11, 33, Tone(bark, 2)); set(16, 33, Tone(bark, 1))
            val leaf = Ramps.of(0xFF4FAE5A.toInt())
            listOf(Triple(7f, 15f, 6f), Triple(21f, 15f, 6f), Triple(14f, 13f, 7f), Triple(9f, 8f, 6f), Triple(19f, 7f, 6.5f), Triple(14f, 4.5f, 5f)).forEach { (x, y, r) -> ball(x, y, r, r * 0.9f, leaf) }
            listOf(6 to 13, 11 to 8, 19 to 11, 22 to 16, 15 to 15, 17 to 4, 8 to 17).forEach { (x, y) ->
                set(x, y, mat(0xFFE2435F.toInt())); set(x + 1, y, mat(0xFFE2435F.toInt())); set(x, y + 1, mat(0xFFE2435F.toInt())); set(x + 1, y + 1, mat(0xFFE2435F.toInt()))
            }
        }
        "well" -> PixelCanvas(26, 30).apply {
            val st = Ramps.of(STONE)
            for (y in 18..29) for (x in 2..23) set(x, y, Tone(st, if ((x + (y / 3) * 3) % 6 == 0 || y % 3 == 0) 1 else if (x < 6) 3 else 2))
            rect(2, 17, 23, 18, Tone(st, 3)); rect(4, 18, 21, 19, Flat(0xFF1A1428.toInt()))
            rect(4, 6, 5, 17, mat(WOOD)); rect(20, 6, 21, 17, mat(WOOD))
            val roof = mat(0xFFC8553D.toInt())
            tri(0f, 7f, 26f, 7f, 13f, 0f, roof); rect(0, 7, 25, 7, tone(0xFFC8553D.toInt(), 1))
            rect(6, 9, 19, 9, mat(DARK)); line(12, 10, 12, 14, Flat(0xFFE9E1D4.toInt()))
            rect(10, 14, 14, 17, mat(0xFF8A5A36.toInt())); rect(10, 14, 14, 14, mat(0xFF8E99A6.toInt()))
        }
        "swing" -> PixelCanvas(28, 30).apply {
            val bark = mat(0xFF7A5034.toInt())
            line(3, 29, 8, 0, bark, 2); line(24, 29, 19, 0, bark, 2); rect(4, 0, 23, 1, bark)
            val sway = if (frame == 0) -1 else 1
            line(10, 2, 10 + sway, 21, Flat(0xFFE9E1D4.toInt())); line(17, 2, 17 + sway, 21, Flat(0xFFE9E1D4.toInt()))
            rect(8 + sway, 21, 19 + sway, 22, mat(0xFFC8693D.toInt()))
        }
        "pond" -> PixelCanvas(34, 10).apply {
            ellipse(17f, 5f, 17f, 5f, Flat(0xFF8E94A8.toInt()))
            ellipse(17f, 5f, 15f, 3.8f, Flat(0xFF3E8FC9.toInt()))
            ellipse(17f, 5.5f, 11f, 2.2f, Flat(0xFF2A6FB0.toInt()))
            val sh = if (frame == 0) 0 else 2
            rect(9 + sh, 3, 13 + sh, 3, Flat(0xFFBFE6FF.toInt())); rect(20 - sh, 6, 23 - sh, 6, Flat(0xFFBFE6FF.toInt()))
            ellipse(25f, 4f, 2.2f, 1.2f, mat(0xFF4FAE5A.toInt())); set(25, 3, Flat(0xFFF28DB2.toInt()))
            ellipse(7f, 6f, 1.8f, 1f, mat(0xFF4FAE5A.toInt()))
        }
        "garden_shed" -> PixelCanvas(30, 30).apply {
            val planks = Ramps.of(0xFFB07A45.toInt())
            for (y in 11..29) for (x in 2..27) set(x, y, Tone(planks, if (x % 4 == 0) 1 else if (x < 6) 3 else 2))
            val roof = Ramps.of(0xFF5A6E9E.toInt())
            for (y in 0..11) for (x in 0..29) {
                if (x in (15 - (y * 1.3f).toInt())..(14 + (y * 1.3f).toInt())) set(x, y, Tone(roof, if (x < 15) 3 else 1))
            }
            rect(0, 11, 29, 11, Tone(roof, 1))
            rect(11, 17, 18, 29, mat(0xFF6B4630.toInt())); set(17, 23, mat(GOLD))
            rect(4, 15, 8, 19, Flat(0xFF3A4A6A.toInt())); rect(4, 15, 8, 15, mat(WOOD)); set(5, 16, Flat(0xFF8CC8F5.toInt()))
            rect(21, 15, 25, 19, Flat(0xFF3A4A6A.toInt())); rect(21, 15, 25, 15, mat(WOOD)); set(22, 16, Flat(0xFF8CC8F5.toInt()))
            line(26, 29, 28, 20, mat(0xFF8A5A36.toInt())); rect(27, 18, 29, 19, mat(0xFF8E99A6.toInt()))
        }
        "fountain" -> PixelCanvas(30, 28).apply {
            val st = mat(STONE)
            ellipse(15f, 23f, 14f, 4.5f, st); rect(1, 19, 28, 23, st)
            ellipse(15f, 19.5f, 12f, 2.5f, Flat(0xFF3E8FC9.toInt()))
            rect(13, 9, 16, 19, st); ellipse(15f, 9f, 6f, 2f, st)
            ellipse(15f, 8.5f, 4.5f, 1.2f, Flat(0xFF5EB0E8.toInt()))
            val water = Flat(0xFFBFE6FF.toInt())
            val f = frame
            for (k in 0..5) {
                set(15 - k + f, 2 + k * k / 4, water); set(15 + k - f, 2 + k * k / 4, water)
                set(10 - k / 2, 10 + k * 2, water); set(20 + k / 2, 10 + k * 2, water)
            }
            set(15, 0 + f, water); set(15, 1, water)
        }
        "windmill" -> PixelCanvas(34, 40).apply {
            val wall = Ramps.of(0xFFE9E1D4.toInt())
            for (y in 14..39) {
                val half = 5 + (y - 14) / 4
                for (x in 17 - half..16 + half) set(x, y, Tone(wall, if (x < 17 - half + 2) 3 else if (x > 14 + half) 1 else 2))
            }
            tri(9f, 15f, 25f, 15f, 17f, 8f, mat(0xFFC8553D.toInt()))
            rect(15, 32, 18, 39, mat(0xFF6B4630.toInt())); rect(15, 22, 18, 25, Flat(0xFF3A4A6A.toInt()))
            val blade = mat(0xFFF6EBDD.toInt())
            val a0 = frame * 0.39
            for (k in 0..3) {
                val a = a0 + k * Math.PI / 2
                val dx = Math.cos(a)
                val dy = Math.sin(a)
                for (r in 2..15) {
                    val x = (17 + dx * r).toInt()
                    val y = (13 + dy * r).toInt()
                    set(x, y, mat(0xFF8A5A36.toInt()))
                    if (r > 4) {
                        set((x - dy * 1.5).toInt(), (y + dx * 1.5).toInt(), blade); set((x - dy * 2.5).toInt(), (y + dx * 2.5).toInt(), blade)
                    }
                }
            }
            ellipse(17f, 13f, 1.6f, 1.6f, mat(DARK))
        }
        // ---- Garden: small ----
        "flower_bed" -> PixelCanvas(20, 10).apply {
            rect(0, 6, 19, 9, mat(0xFF8A5A36.toInt())); rect(1, 6, 18, 6, Flat(0xFF5A3A24.toInt()))
            val cols = intArrayOf(0xFFE2435F.toInt(), 0xFFFFE066.toInt(), 0xFFB79CFF.toInt(), 0xFFF28DB2.toInt(), 0xFFFFFFFF.toInt())
            for (k in 0..5) {
                val x = 2 + k * 3
                line(x, 6, x, 3 + (k % 2), mat(0xFF4FAE5A.toInt()))
                val c = Ramps.of(cols[k % cols.size])
                set(x, 1 + (k % 2), Tone(c, 3)); set(x - 1, 2 + (k % 2), Tone(c, 2)); set(x + 1, 2 + (k % 2), Tone(c, 2)); set(x, 3 + (k % 2), Tone(c, 1)); set(x, 2 + (k % 2), Flat(0xFFFFE066.toInt()))
            }
        }
        "mailbox" -> PixelCanvas(12, 18).apply {
            rect(5, 8, 6, 17, mat(WOOD))
            ellipse(5.5f, 4f, 5f, 3.5f, mat(0xFF4F7BD9.toInt())); rect(1, 4, 10, 8, mat(0xFF4F7BD9.toInt()))
            rect(1, 7, 1, 8, mat(DARK)); rect(10, 1, 11, 2, mat(0xFFE2435F.toInt())); rect(10, 1, 10, 6, mat(0xFF8E99A6.toInt()))
        }
        "gnome" -> PixelCanvas(10, 16).apply {
            tri(5f, 0f, 1f, 7f, 9f, 7f, mat(0xFFE2435F.toInt()))
            ellipse(5f, 8f, 2.6f, 2f, mat(0xFFF3C9A8.toInt())); set(5, 8, Flat(0xFFE0A87E.toInt()))
            tri(2f, 9f, 8f, 9f, 5f, 14f, mat(0xFFF6F2EA.toInt()))
            rect(2, 11, 8, 14, mat(0xFF4F7BD9.toInt())); tri(2f, 9f, 8f, 9f, 5f, 13f, mat(0xFFF6F2EA.toInt()))
            rect(2, 15, 4, 15, mat(DARK)); rect(6, 15, 8, 15, mat(DARK))
        }
        "pumpkin_patch" -> PixelCanvas(20, 10).apply {
            val pk = Ramps.of(0xFFF08A2E.toInt())
            ball(5f, 6f, 4.5f, 3.5f, pk, texture = false); ball(14f, 6.5f, 5f, 3.4f, pk, texture = false); ball(10f, 8f, 3f, 2f, pk, texture = false)
            listOf(5, 14).forEach { x -> set(x, 3, Tone(pk, 1)); set(x, 4, Tone(pk, 1)); set(x, 2, mat(0xFF5DAE4F.toInt())) }
            line(1, 9, 19, 9, mat(0xFF4FAE5A.toInt())); set(8, 1, mat(0xFF4FAE5A.toInt())); set(9, 2, mat(0xFF4FAE5A.toInt()))
        }
        "birdhouse" -> PixelCanvas(12, 20).apply {
            rect(5, 9, 6, 19, mat(WOOD))
            rect(2, 3, 9, 10, mat(0xFFF2D27A.toInt())); tri(0f, 4f, 12f, 4f, 6f, -1f, mat(0xFF4FAE5A.toInt()))
            ellipse(5.5f, 6.5f, 1.4f, 1.4f, Flat(0xFF2A1F38.toInt())); rect(5, 9, 6, 9, mat(DARK))
        }
        "lamp_post" -> PixelCanvas(10, 26).apply {
            val iron = mat(0xFF3A3550.toInt())
            rect(4, 6, 5, 24, iron); rect(2, 24, 7, 25, iron)
            rect(1, 0, 8, 1, iron); rect(2, 2, 7, 5, mat(0xFFFFE066.toInt(), glows = true)); rect(2, 6, 7, 6, iron)
            set(4, 3, Flat(0xFFFFF8DC.toInt()))
        }
        "bench" -> PixelCanvas(22, 11).apply {
            rect(0, 0, 21, 1, mat(WOOD)); rect(0, 3, 21, 3, mat(WOOD)); rect(0, 6, 21, 7, mat(0xFFB07A45.toInt()))
            val iron = mat(0xFF3A3550.toInt())
            rect(1, 0, 2, 10, iron); rect(19, 0, 20, 10, iron)
        }
        "beehive" -> PixelCanvas(14, 18).apply {
            rect(6, 13, 7, 17, mat(WOOD)); rect(2, 16, 11, 17, mat(WOOD))
            val hive = Ramps.of(0xFFE0B341.toInt())
            listOf(Triple(7f, 11f, 5f), Triple(7f, 7.5f, 4.2f), Triple(7f, 4.5f, 3f), Triple(7f, 2f, 1.8f)).forEach { (x, y, r) -> ball(x, y, r, 1.9f, hive, texture = false) }
            rect(6, 10, 7, 11, Flat(0xFF2A1F38.toInt()))
            val b = Flat(0xFF2E2433.toInt())
            val y = Flat(0xFFFFE066.toInt())
            listOf(1 to 3, 12 to 6, 2 to 12).forEach { (x, yy) -> val ox = if (frame == 0) 0 else 1; set(x + ox, yy - ox, y); set(x + 1 + ox, yy - ox, b) }
        }
        "garden_scarecrow" -> PixelCanvas(18, 26).apply {
            rect(8, 8, 9, 25, mat(0xFF7A5034.toInt())); rect(1, 11, 16, 12, mat(0xFF7A5034.toInt()))
            tri(8.5f, 9f, 3f, 21f, 14f, 21f, mat(0xFF4F7BD9.toInt())); rect(4, 11, 13, 15, mat(0xFF4F7BD9.toInt()))
            rect(10, 16, 12, 18, mat(0xFFE2435F.toInt()))
            ellipse(8.5f, 5.5f, 3.6f, 3.4f, mat(0xFFD9B98A.toInt())); set(7, 5, EYE); set(10, 5, EYE); rect(7, 7, 10, 7, EYE)
            rect(3, 2, 14, 2, mat(0xFFE8C15A.toInt())); rect(5, 0, 12, 1, mat(0xFFE8C15A.toInt()))
            listOf(0, 17).forEach { x -> set(x, 12, mat(0xFFE8C15A.toInt())) }
        }
        // ---- Garden: fences and paths (tiles) ----
        "fence_picket" -> PixelCanvas(24, 12).apply {
            val w = Ramps.of(0xFFF6F2EA.toInt())
            for (x in 0..21 step 4) { rect(x, 1, x + 2, 11, Tone(w, 2)); rect(x, 1, x, 11, Tone(w, 3)); rect(x + 2, 2, x + 2, 11, Tone(w, 1)); set(x + 1, 0, Tone(w, 3)) }
            rect(0, 4, 23, 4, Tone(w, 2)); rect(0, 8, 23, 8, Tone(w, 2))
        }
        "fence_hedge" -> PixelCanvas(24, 12).apply {
            val l = Ramps.of(0xFF3F8A4A.toInt())
            listOf(4f, 11f, 18f, 24f).forEach { x -> ball(x, 6f, 5f, 5.5f, l) }
            ball(8f, 4f, 3.5f, 3f, l); ball(16f, 4f, 3.5f, 3f, l); rect(0, 11, 23, 11, Tone(l, 1))
        }
        "fence_stone" -> PixelCanvas(24, 12).apply {
            val st = Ramps.of(0xFF9AA0B5.toInt())
            for (y in 2..11) for (x in 0..23) {
                val off = if ((y / 3) % 2 == 0) 0 else 3
                set(x, y, Tone(st, if (y % 3 == 1 || (x + off) % 6 == 0) 1 else if (y % 3 == 2) 3 else 2))
            }
            rect(0, 0, 23, 1, Tone(st, 3)); rect(0, 1, 23, 1, Tone(st, 2))
            set(4, 6, Tone(Ramps.of(0xFF6DBA5A.toInt()), 3)); set(15, 9, Tone(Ramps.of(0xFF6DBA5A.toInt()), 3))
        }
        "fence_iron" -> PixelCanvas(24, 14).apply {
            val iron = Ramps.of(0xFF3A3550.toInt())
            for (x in 1..23 step 3) { rect(x, 2, x, 13, Tone(iron, 2)); set(x, 1, Tone(Ramps.of(GOLD), 3)); set(x, 0, Tone(Ramps.of(GOLD), 4)) }
            rect(0, 4, 23, 4, Tone(iron, 2)); rect(0, 11, 23, 11, Tone(iron, 2))
        }
        "path_dirt" -> PixelCanvas(24, 6).apply {
            for (y in 0..5) for (x in 0..23) set(x, y, Flat(if (noise(x, y) > 0.8f) 0xFF9A7048.toInt() else if (noise(x + 3, y) > 0.85f) 0xFFC8A070.toInt() else 0xFFB08452.toInt()))
        }
        "path_stone" -> PixelCanvas(24, 6).apply {
            for (y in 0..5) for (x in 0..23) set(x, y, Flat(0xFF6DBA5A.toInt()))
            listOf(Triple(3f, 3f, 3f), Triple(10f, 2.5f, 3.2f), Triple(17f, 3.2f, 2.8f), Triple(22.5f, 2.5f, 2.5f)).forEach { (x, y, r) -> ball(x, y, r, 2.4f, Ramps.of(0xFFB4B8C8.toInt()), texture = false, top = 3) }
        }
        "path_brick" -> PixelCanvas(24, 6).apply {
            for (y in 0..5) for (x in 0..23) {
                val off = if ((y / 2) % 2 == 0) 0 else 2
                set(x, y, Flat(if (y % 2 == 1 || (x + off) % 4 == 0) 0xFF8A4A3A.toInt() else if ((x / 4 + y) % 3 == 0) 0xFFC2614A.toInt() else 0xFFB5563F.toInt()))
            }
        }
        else -> null
    }
}
