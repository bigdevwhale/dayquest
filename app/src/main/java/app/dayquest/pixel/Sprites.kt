package app.dayquest.pixel

import app.dayquest.game.BossShape
import app.dayquest.game.Catalog
import app.dayquest.game.Food
import app.dayquest.game.GearShape
import app.dayquest.game.Potion
import app.dayquest.game.Species

/** What the hero looks like: everything the sprite depends on, so it doubles as a cache key. */
data class Look(
    val skin: Int,
    val hairStyle: Int,
    val hairColor: Int,
    val weapon: String,
    val armor: String,
    val head: String,
    val shield: String,
)

object Sprites {
    private val cache = HashMap<String, Sprite>()

    fun cached(key: String, build: () -> Sprite): Sprite = synchronized(cache) { cache[key] } ?: build().also { s ->
        synchronized(cache) { cache[key] = s }
    }

    private val EYE = Flat(0xFF2A1F38.toInt())
    private val WHITE = Flat(0xFFFFFFFF.toInt())
    private fun mat(c: Int, glows: Boolean = false) = Mat(Ramps.of(c), glows)

    // ---- Hero ----------------------------------------------------------------------------

    const val HERO_W = 20
    const val HERO_H = 24

    private val BODY = listOf(
        "....................",
        "....................",
        "....................",
        "....................",
        "....................",
        ".......ssssss.......",
        "......ssssssss......",
        "......ssssssss......",
        "......ssssssss......",
        "......sWKssWKs......",
        "......sKKssKKs......",
        "......pssmmssp......",
        ".......ssssss.......",
        "........ssss........",
        "......cccccccc......",
        ".....cccccccccc.....",
        "....cc.cccccc.cc....",
        "....cc.cccccc.cc....",
        "....ss.llllll.ss....",
        ".......dddddd.......",
        ".......dd..dd.......",
        ".......dd..dd.......",
        "......bbb..bbb......",
        "....................",
    )

    private val HAIR_TOP = listOf(
        ".......hhhhhh.......",
        "......hhhhhhhh......",
        ".....hhhhhhhhhh.....",
        ".....hhhhhhhhhh.....",
        ".....hh.h....hh.....",
        ".....h........h.....",
    )

    /** Hair styles as overlays from row 2; '.' keeps what is below. */
    private val HAIR: List<List<String>> = listOf(
        // short
        listOf("....................", "....................") + HAIR_TOP,
        // long
        listOf("....................", "....................") + HAIR_TOP + listOf(
            ".....h........h.....",
            ".....h........h.....",
            ".....hh......hh.....",
            ".....hhh....hhh.....",
            ".....hh......hh.....",
            ".....h........h.....",
        ),
        // bun
        listOf("........hhhh........", ".......hhhhhh.......") + HAIR_TOP,
        // spiky
        listOf("......h..h..h.......", "......hh.hh.hhh.....") + HAIR_TOP,
        // bob
        listOf("....................", "....................") + HAIR_TOP.take(5) + listOf(
            ".....hh......hh.....",
            ".....hh......hh.....",
            ".....hhh....hhh.....",
            "......h......h......",
        ),
        // ponytail
        listOf("....................", "....................") + HAIR_TOP.take(3) + listOf(
            ".....hhhhhhhhhhh....",
            ".....hh.h....hhhh...",
            ".....h........h.hh..",
            "................hh..",
            "................h...",
        ),
    )

    private val HEAD_GEAR = mapOf(
        GearShape.CAP to (3 to listOf(
            ".........GG.........",
            ".......gggggg.......",
            "......gggggggg......",
            ".....gggggggggg.....",
            "....GGGGGGGGGGGG....",
        )),
        GearShape.HELM to (3 to listOf(
            ".........GG.........",
            ".......gggggg.......",
            "......gggggggg......",
            ".....gggggggggg.....",
            ".....GGGGGGGGGG.....",
            ".....gg......gg.....",
            ".....gg......gg.....",
            ".....g........g.....",
        )),
        GearShape.WIZARD to (0 to listOf(
            "............gg......",
            "...........ggg......",
            "..........gggg......",
            ".........ggGgg......",
            "........gggggg......",
            ".......gggggggg.....",
            "......GGGGGGGGGG....",
            "...gggggggggggggg...",
        )),
        GearShape.HORNED to (2 to listOf(
            "...G............G...",
            "...GG..........GG...",
            "....GG.gggggg.GG....",
            ".....GggggggggG.....",
            ".....gggggggggg.....",
            ".....gggggggggg.....",
            ".....gg......gg.....",
        )),
        GearShape.FLOWER to (4 to listOf(
            ".....g..........g...",
            "....gGgGgGgGgGgG....",
        )),
        GearShape.SUNHAT to (3 to listOf(
            ".......gggggg.......",
            "......gggggggg......",
            "......GGGGGGGG......",
            "..gggggggggggggggg..",
            "...gggggggggggggg...",
        )),
        GearShape.PUMPKIN to (1 to listOf(
            ".........GG.........",
            "........GG..........",
            ".....gggggggggg.....",
            "....gggggggggggg....",
            "....gggggggggggg....",
            "....gggggggggggg....",
            ".....gggggggggg.....",
        )),
        GearShape.BEANIE to (1 to listOf(
            ".........GG.........",
            "........GGGG........",
            ".......gggggg.......",
            "......gggggggg......",
            ".....gggggggggg.....",
            ".....GGGGGGGGGG.....",
            ".....GGGGGGGGGG.....",
        )),
        GearShape.HOOD to (2 to listOf(
            ".......gggggg.......",
            "......gggggggg......",
            ".....gggggggggg.....",
            ".....gggggggggg.....",
            ".....gG......Gg.....",
            ".....g........g.....",
            ".....g........g.....",
            ".....g........g.....",
            ".....gg......gg.....",
            "....ggg......ggg....",
        )),
        GearShape.BANDANA to (5 to listOf(
            ".....gggggggggg.....",
            ".....GgGgGgGgGggg...",
            "...............g.g..",
            "................g...",
        )),
        GearShape.CIRCLET to (5 to listOf(
            ".........GG.........",
            ".....gggggGggggg....",
        )),
        GearShape.FEATHER to (1 to listOf(
            "...............GG...",
            "..............GGG...",
            "........gggggGG.....",
            "......gggggggGg.....",
            ".....ggggggggggg....",
            "....GGgggggggggggg..",
        )),
        GearShape.CROWN to (2 to listOf(
            "......g..gg..g......",
            "......gggggggg......",
            "......gggGGggg......",
        )),
    )

    private val ARMOR_GEAR = mapOf(
        GearShape.PLATE to (14 to listOf(
            "....AAaaaaaaaaAA....",
            "....AAaaaaaaaaAA....",
            "....aa.aaAAaa.aa....",
            "....aa.aaaaaa.aa....",
        )),
        GearShape.ROBE to (19 to listOf(
            ".......aaAAaa.......",
            "......aaaAAaaa......",
            "......aaaAAaaa......",
        )),
    )

    /** A cape hangs behind the body; the cloak's clasp sits at the collar. */
    private val CAPE = listOf(
        "....aaaaaaaaaaaa....",
        "...aaa........aaa...",
        "...aa..........aa...",
        "...aa..........aa...",
        "...aaa........aaa...",
        "...aaa........aaa...",
        "...aaaa......aaaa...",
        "....aaa......aaa....",
    )

    private val WEAPONS = mapOf(
        GearShape.DAGGER to (11 to listOf(
            "................w...",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "...............WWWW.",
            "................W...",
        )),
        GearShape.SPEAR to (0 to listOf(
            "................w...",
            "...............www..",
            "...............www..",
            "...............www..",
            "................w...",
            "...............WWW..",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
        )),
        GearShape.HAMMER to (4 to listOf(
            "..............wwwww.",
            ".............wwwwwww",
            ".............wwwwwww",
            ".............wwwwwww",
            "..............wwwww.",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
        )),
        GearShape.BOW to (6 to listOf(
            "...............Ww...",
            "...............W.w..",
            "...............W..w.",
            "...............W..w.",
            "...............W...w",
            "...............W...w",
            "...............W...w",
            "...............W...w",
            "...............W...w",
            "...............W..w.",
            "...............W..w.",
            "...............W.w..",
            "...............Ww...",
        )),
        GearShape.SCYTHE to (0 to listOf(
            "..........wwwwwwW...",
            "........www..wwwW...",
            "........w.......W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
        )),
        GearShape.WAND to (11 to listOf(
            "...............w.w..",
            "................w...",
            "...............w.w..",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
        )),
        GearShape.SWORD to (7 to listOf(
            "................w...",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "................ww..",
            "...............WWWW.",
            "................WW..",
            "................W...",
        )),
        GearShape.STAFF to (3 to listOf(
            "...............ww...",
            "..............wwww..",
            "..............wwww..",
            "...............ww...",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
            "...............W....",
        )),
        GearShape.AXE to (5 to listOf(
            "................W...",
            "..............wwWw..",
            ".............wwwWww.",
            ".............wwwWww.",
            "..............wwWw..",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
            "................W...",
        )),
    )

    private val SHIELDS = mapOf(
        GearShape.ROUND to (15 to listOf(
            "..zzz...............",
            ".zzZzz..............",
            ".zZZZz..............",
            ".zzZzz..............",
            "..zzz...............",
        )),
        GearShape.KITE to (14 to listOf(
            ".zzzzz..............",
            ".zZZZz..............",
            ".zzZzz..............",
            ".zzZzz..............",
            "..zZz...............",
            "...z................",
        )),
        GearShape.TOWER to (12 to listOf(
            "zzzzzz..............",
            "zZzzZz..............",
            "zzZZzz..............",
            "zzZZzz..............",
            "zZzzZz..............",
            "zzzzzz..............",
            "zzzzzz..............",
            ".zzzz...............",
        )),
        GearShape.LANTERN to (16 to listOf(
            "..z.................",
            ".zzz................",
            ".ZZZ................",
            ".ZZZ................",
            ".zzz................",
        )),
        GearShape.ORB to (14 to listOf(
            "..ZZ................",
            ".ZZZZ...............",
            ".ZZZZ...............",
            "..ZZ................",
            ".zzzz...............",
        )),
        GearShape.BOOK to (15 to listOf(
            ".zzzzz..............",
            ".zzZzz..............",
            ".zZZZz..............",
            ".zzZzz..............",
            ".zzzzz..............",
        )),
    )

    fun hero(look: Look, blink: Boolean = false): Sprite = cached("hero:$look:$blink") {
        val c = PixelCanvas(HERO_W, HERO_H)
        val skin = Catalog.skins[look.skin.coerceIn(0, Catalog.skins.size - 1)]
        val hair = Catalog.hairColors[look.hairColor.coerceIn(0, Catalog.hairColors.size - 1)]
        val armor = Catalog.gear(look.armor) ?: Catalog.gear("tunic")!!
        val weapon = Catalog.gear(look.weapon)
        val head = Catalog.gear(look.head)
        val shield = Catalog.gear(look.shield)
        val skinInk = mat(skin)

        if (armor.shape == GearShape.CLOAK) c.grid(CAPE, mapOf('a' to mat(armor.secondary)), dy = 14)
        weapon?.let { g ->
            val (dy, rows) = WEAPONS[g.shape] ?: return@let
            c.grid(rows, mapOf('w' to mat(g.primary, g.glow), 'W' to mat(g.secondary)), dy = dy)
        }
        c.grid(
            BODY,
            mapOf(
                's' to skinInk, 'K' to EYE, 'W' to WHITE,
                'p' to Flat(Color.mix(skin, 0xFFF27C9B.toInt(), 0.45f)),
                'm' to Flat(Color.mix(skin, 0xFF7A2E3A.toInt(), 0.6f)),
                'c' to mat(armor.primary), 'l' to mat(armor.secondary),
                'd' to mat(0xFF4A4262.toInt()), 'b' to mat(0xFF6B4630.toInt()),
            ),
        )
        if (armor.shape == GearShape.CLOAK) {
            // Collar and a golden clasp
            c.rect(6, 14, 13, 14, mat(armor.secondary)); c.set(9, 14, mat(0xFFF2C14E.toInt())); c.set(10, 14, mat(0xFFF2C14E.toInt()))
        }
        ARMOR_GEAR[armor.shape]?.let { (dy, rows) ->
            c.grid(rows, mapOf('a' to (c[8, 15] ?: mat(armor.primary)), 'A' to mat(armor.secondary)), dy = dy)
        }
        val hairRows = HAIR[look.hairStyle.coerceIn(0, HAIR.size - 1)]
        c.grid(hairRows, mapOf('h' to mat(hair)), dy = 2)
        head?.let { g ->
            val (dy, rows) = HEAD_GEAR[g.shape] ?: return@let
            c.grid(rows, mapOf('g' to mat(g.primary, g.glow), 'G' to mat(g.secondary, g.glow)), dy = dy)
        }
        shield?.let { g ->
            val (dy, rows) = SHIELDS[g.shape] ?: return@let
            c.grid(rows, mapOf('z' to mat(g.primary, g.glow), 'Z' to mat(g.secondary, g.glow)), dy = dy)
        }
        if (blink) {
            for (x in listOf(7, 8, 11, 12)) c.set(x, 9, skinInk)
        }
        c.render()
    }

    /** Just the head and shoulders, for avatars in lists. */
    fun heroBust(look: Look): Sprite = cached("bust:$look") {
        val full = hero(look)
        val h = 17
        Sprite(full.w, h, full.px.copyOfRange(0, full.w * h))
    }

    // ---- Pets ----------------------------------------------------------------------------

    const val PET_W = 14
    const val PET_H = 14

    private val PETS = mapOf(
        Species.FOX to listOf(
            "..a.......a...",
            ".aba.....aba..",
            ".abaaaaaaaba..",
            "aaaaaaaaaaaaa.",
            "aaWaaaaaaaWaa.",
            "aaKaaaaaaaKaa.",
            "abbbbbnbbbbba.",
            ".abbbbbbbbba..",
            "...aaaaaaa...b",
            "...abbbbba..ab",
            "...abbbbbaaaa.",
            "...aa.a.aa....",
        ),
        Species.CAT to listOf(
            ".a.........a..",
            ".aa.......aa..",
            ".abaaaaaaaba..",
            "aaaaadadaaaaa.",
            "aaWaaaaaaaWaa.",
            "aaKaaaaaaaKaa.",
            "aaaaaanaaaaaa.",
            ".aaaabbbaaaa..",
            "...aaaaaaa..a.",
            "...abbbbba..a.",
            "...abbbbbaaaa.",
            "...aa.a.aa....",
        ),
        Species.OWL to listOf(
            "..a.......a...",
            "..aa.....aa...",
            "..aaaaaaaaa...",
            ".aaaaaaaaaaa..",
            ".abWbaaabWba..",
            ".abKbaaabKba..",
            ".abbbacabbba..",
            ".aabbbcbbbaa..",
            "..abbbbbbba...",
            "..adbbbbbda...",
            "...aabbbaa....",
            "....c...c.....",
        ),
        Species.SLIME to listOf(
            "..............",
            "......aa......",
            ".....aWaa.....",
            "....aaaaaa....",
            "...aaaaaaaa...",
            "..aaaaaaaaaa..",
            "..aaWaaaaWaa..",
            ".aaaKaaaaKaaa.",
            ".aaaaannaaaaa.",
            "aaaaaaaaaaaaaa",
            ".aaaaaaaaaaaa.",
            "..............",
        ),
        Species.BUNNY to listOf(
            "...a.....a....",
            "..aba...aba...",
            "..aba...aba...",
            "..aba...aba...",
            "..aaaaaaaaa...",
            ".aaaaaaaaaaa..",
            ".aaWaaaaaWaa..",
            ".aaKaaaaaKaa..",
            ".abaaanaaaba..",
            "..aaaaaaaaa...",
            "..aabbbbbaaa..",
            "...aa...aa....",
        ),
        Species.WOLF to listOf(
            ".a.........a..",
            ".aa.......aa..",
            ".aba.....aba..",
            "aaaaaaaaaaaaa.",
            "adWaaaaaaaWda.",
            "aaKaaaaaaaKaa.",
            "aabbbbnbbbbaa.",
            ".abbbbbbbbba..",
            "...aaaaaaa....",
            "...abbbbba.aa.",
            "...abbbbbaaab.",
            "...aa.a.aa....",
        ),
        Species.TURTLE to listOf(
            "..............",
            "..............",
            "..............",
            "....aaaaa.....",
            "...aadadaa....",
            "..aadaaadaa...",
            ".aaaadadaaa.bb",
            ".aaaaadaaaabKb",
            "ddddddddddbbb.",
            ".bb.....bb....",
            "..............",
            "..............",
        ),
        Species.DRAGON to listOf(
            "..c.......c...",
            "..ca.....ac...",
            "...aaaaaaa....",
            "..aaaaaaaaa...",
            "..aWaaaaaWa...",
            "..aKaaaaaKa...",
            "..aanaaanaa...",
            "d..abbbbba..d.",
            "dd.abbbbba.dd.",
            "ddaabbbbbaadd.",
            "...abbbbba..aa",
            "...aa...aaaaa.",
        ),
    )

    private val PET_CROWN = listOf(
        ".....g.g.g....",
        ".....ggggg....",
    )

    fun petColors(s: Species, p: Potion): Pair<Int, Int> {
        val main = p.color ?: s.color
        val belly = when {
            p.belly != null -> p.belly
            p.color == null -> s.belly
            s == Species.SLIME -> Color.mix(main, 0xFFFFFFFF.toInt(), 0.55f)
            else -> Color.mix(s.belly, main, 0.25f)
        }
        return main to belly
    }

    fun pet(s: Species, p: Potion, grown: Boolean = false, blink: Boolean = false): Sprite = cached("pet:${s.key}:${p.key}:$grown:$blink") {
        val c = PixelCanvas(PET_W, PET_H)
        val (main, belly) = petColors(s, p)
        val mainRamp = Ramps.of(main)
        val legend = mapOf(
            'a' to Mat(mainRamp), 'b' to mat(belly), 'd' to Tone(mainRamp, 1),
            'K' to EYE, 'W' to WHITE, 'n' to Flat(Color.mix(main, 0xFF3A2030.toInt(), 0.7f)),
            'c' to mat(if (s == Species.OWL) 0xFFF2A03D.toInt() else belly),
        )
        val rows = PETS.getValue(s).let { r -> if (blink) r.map { it.replace('W', 'B') } else r }
        c.grid(rows, legend + ('B' to Tone(mainRamp, 1)), dy = 2)
        if (blink) {
            // Closed eyes: the pupil row turns into a short lash line.
            for (y in 0 until PET_H) for (x in 0 until PET_W) if (c[x, y] === EYE && c[x, y - 1] is Tone) c.set(x, y - 1, Mat(mainRamp))
        }
        if (grown) c.grid(PET_CROWN, mapOf('g' to mat(0xFFF2C14E.toInt(), glows = true)))
        c.render()
    }

    /**
     * A saddled pet: the same creature drawn twice as big (shading and outline stay one pixel thin,
     * so it reads as a larger beast, not a blurry zoom), with a blanket and saddle across its back.
     */
    fun mount(s: Species, p: Potion, blink: Boolean = false): Sprite = cached("mount:${s.key}:${p.key}:$blink") {
        val c = PixelCanvas(PET_W * 2, PET_H * 2)
        val (main, belly) = petColors(s, p)
        val mainRamp = Ramps.of(main)
        val legend = mapOf(
            'a' to Mat(mainRamp), 'b' to mat(belly), 'd' to Tone(mainRamp, 1),
            'K' to EYE, 'W' to WHITE, 'n' to Flat(Color.mix(main, 0xFF3A2030.toInt(), 0.7f)),
            'c' to mat(if (s == Species.OWL) 0xFFF2A03D.toInt() else belly), 'B' to Tone(mainRamp, 1),
        )
        val rows = PETS.getValue(s).let { r -> if (blink) r.map { it.replace('W', 'B') } else r }
        val big = rows.flatMap { row -> row.map { "$it$it" }.joinToString("").let { listOf(it, it) } }
        c.grid(big, legend, dy = 2)
        // Each 2×2 pupil gets a catch-light in its top-left corner, so the bigger face stays alive.
        val shines = (0 until c.h).flatMap { y -> (0 until c.w).map { x -> x to y } }
            .filter { (x, y) -> c[x, y] === EYE && c[x - 1, y] !== EYE && c[x, y - 1] !== EYE }
        shines.forEach { (x, y) -> c.set(x, y, WHITE) }
        // The saddle sits on the back: just below the head for most, on top of the shell for the turtle.
        val top = if (s == Species.TURTLE) 10 else 20
        val blanket = mat(0xFFC8553D.toInt())
        val trim = mat(0xFFF2C14E.toInt())
        val leather = mat(0xFF7A4A2A.toInt())
        val xs = (0 until c.w).filter { c[it, top] != null && c[it, top + 2] != null }
        if (xs.isNotEmpty()) {
            val l = xs.first() + 2
            val r = xs.last() - 2
            for (x in l..r) {
                c.set(x, top, blanket); c.set(x, top + 1, blanket); c.set(x, top + 2, trim)
            }
            val mid = (l + r) / 2
            c.rect(mid - 3, top - 2, mid + 3, top - 1, leather)
            c.rect(mid - 4, top - 3, mid - 3, top - 2, leather); c.rect(mid + 3, top - 3, mid + 4, top - 2, leather)
        }
        c.render()
    }

    /** A dark silhouette for pets not hatched yet: the collection shows what is still out there. */
    fun petSilhouette(s: Species, dark: Boolean): Sprite = cached("sil:${s.key}:$dark") {
        val base = pet(s, Potion.BASE)
        val col = if (dark) 0xFF2B2740.toInt() else 0xFFD9D3E6.toInt()
        Sprite(base.w, base.h, IntArray(base.px.size) { if (Color.a(base.px[it]) == 0) 0 else col })
    }

    // ---- Items ---------------------------------------------------------------------------

    fun egg(s: Species): Sprite = cached("egg:${s.key}") {
        val c = PixelCanvas(9, 11)
        c.ellipse(4.5f, 6f, 3.7f, 4.8f, mat(Color.mix(s.belly, 0xFFFFFFFF.toInt(), 0.35f)))
        val spot = mat(s.color)
        listOf(5 to 3, 3 to 5, 4 to 5, 6 to 7, 2 to 8, 5 to 9).forEach { (x, y) -> c.set(x, y, spot) }
        c.render()
    }

    private val BOTTLE = listOf(
        "...kk...",
        "...gg...",
        "..gllg..",
        ".gWlllg.",
        ".gllllg.",
        ".gllllg.",
        "..gggg..",
    )

    fun potion(color: Int, key: String = color.toString()): Sprite = cached("potion:$key") {
        val c = PixelCanvas(8, 7)
        c.grid(BOTTLE, mapOf('k' to mat(0xFFA0714A.toInt()), 'g' to mat(0xFFCFD8EA.toInt()), 'l' to mat(color, glows = true), 'W' to WHITE))
        c.render()
    }

    fun potion(p: Potion) = potion(p.swatch, p.key)

    fun food(f: Food): Sprite = cached("food:${f.key}") {
        val c = PixelCanvas(10, 9)
        when (f) {
            Food.BERRY -> {
                c.grid(listOf("....gg....", "...g......"), mapOf('g' to mat(0xFF5DAE4F.toInt())))
                val b = mat(0xFF8E4FD1.toInt())
                c.ellipse(3.5f, 4.5f, 2.2f, 2.2f, b); c.ellipse(6.5f, 4.5f, 2.2f, 2.2f, b); c.ellipse(5f, 7f, 2.2f, 2f, b)
                c.set(3, 3, WHITE); c.set(6, 3, WHITE)
            }
            Food.FISH -> {
                c.ellipse(4f, 4.5f, 3.6f, 2.4f, mat(0xFF6FB6E8.toInt()))
                c.tri(7f, 4.5f, 10f, 2f, 10f, 7f, mat(0xFF4F8FD0.toInt()))
                c.set(2, 4, EYE)
            }
            Food.MEAT -> {
                c.ellipse(4f, 4f, 3.4f, 3f, mat(0xFFC8553D.toInt()))
                c.line(6, 6, 8, 8, mat(0xFFF1E6D3.toInt()), 1)
                c.set(9, 8, mat(0xFFF1E6D3.toInt())); c.set(8, 7, mat(0xFFF1E6D3.toInt()))
                c.set(3, 2, mat(0xFFE88A6A.toInt()))
            }
            Food.CAKE -> {
                c.tri(1f, 8f, 9f, 8f, 9f, 3f, mat(0xFFF2D7A6.toInt()))
                c.rect(3, 6, 8, 6, mat(0xFFF28DB2.toInt()))
                c.line(3, 6, 8, 2, mat(0xFFFFF5FA.toInt()))
                c.set(8, 1, mat(0xFFE2435F.toInt()))
            }
        }
        c.render()
    }

    val coin: Sprite get() = cached("coin") {
        val c = PixelCanvas(8, 8)
        c.ellipse(4f, 4f, 3.6f, 3.6f, mat(0xFFF2C14E.toInt()))
        c.rect(3, 2, 4, 5, Tone(Ramps.of(0xFFF2C14E.toInt()), 1))
        c.set(2, 2, WHITE)
        c.render()
    }

    val heart: Sprite get() = cached("heart") {
        val c = PixelCanvas(9, 8)
        c.grid(
            listOf(
                ".hh...hh.",
                "hhhh.hhhh",
                "hWhhhhhhh",
                "hhhhhhhhh",
                ".hhhhhhh.",
                "..hhhhh..",
                "...hhh...",
                "....h....",
            ),
            mapOf('h' to mat(0xFFE5484D.toInt()), 'W' to WHITE),
        )
        c.render()
    }

    val star: Sprite get() = cached("star") {
        val c = PixelCanvas(9, 9)
        c.grid(
            listOf(
                "....s....",
                "...sss...",
                "sssssssss",
                ".sssWsss.",
                "..sssss..",
                "..sssss..",
                ".sss.sss.",
                ".ss...ss.",
                "s.......s",
            ),
            mapOf('s' to mat(0xFFFFC94A.toInt()), 'W' to WHITE),
        )
        c.render()
    }

    val gem: Sprite get() = cached("gem") {
        val c = PixelCanvas(9, 8)
        c.grid(
            listOf(
                "..ggggg..",
                ".gWgggGg.",
                "ggggggGGg",
                ".gggggGg.",
                "..gggGg..",
                "...ggg...",
                "....g....",
            ),
            mapOf('g' to mat(0xFF7C5CFF.toInt()), 'G' to Tone(Ramps.of(0xFF7C5CFF.toInt()), 1), 'W' to WHITE),
        )
        c.render()
    }

    val flame: Sprite get() = cached("flame") {
        val c = PixelCanvas(7, 9)
        c.grid(
            listOf(
                "...o...",
                "..oo...",
                "..ooo.o",
                ".ooooo.",
                "ooyyooo",
                "ooyyyoo",
                "oyyWyyo",
                ".oyyyo.",
                "..ooo..",
            ),
            mapOf('o' to mat(0xFFFF7A3D.toInt()), 'y' to mat(0xFFFFC94A.toInt()), 'W' to Flat(0xFFFFF6D8.toInt())),
        )
        c.render()
    }

    val healthPotion: Sprite get() = potion(0xFFE5484D.toInt(), "health")

    val tombstone: Sprite get() = cached("tomb") {
        val c = PixelCanvas(14, 16)
        val stone = mat(0xFF9AA0B5.toInt())
        c.ellipse(7f, 5f, 5.5f, 5f, stone)
        c.rect(2, 5, 12, 14, stone)
        c.rect(6, 4, 7, 10, Tone(Ramps.of(0xFF9AA0B5.toInt()), 1))
        c.rect(4, 6, 9, 7, Tone(Ramps.of(0xFF9AA0B5.toInt()), 1))
        c.rect(0, 14, 13, 15, mat(0xFF5DAE4F.toInt()))
        c.render()
    }

    val chest: Sprite get() = cached("chest") {
        val c = PixelCanvas(16, 13)
        val wood = mat(0xFFA0643A.toInt())
        val gold = mat(0xFFF2C14E.toInt(), glows = true)
        c.ellipse(8f, 4.5f, 7.5f, 4f, wood)
        c.rect(0, 4, 15, 12, wood)
        c.rect(0, 5, 15, 5, gold)
        c.rect(3, 0, 4, 12, gold); c.rect(11, 0, 12, 12, gold)
        c.rect(7, 5, 8, 7, gold)
        c.render()
    }

    /** The chest with its lid thrown back and light pouring out. */
    val chestOpen: Sprite get() = cached("chest_open") {
        val c = PixelCanvas(16, 15)
        val wood = mat(0xFFA0643A.toInt())
        val gold = mat(0xFFF2C14E.toInt(), glows = true)
        val light = mat(0xFFFFF3B0.toInt(), glows = true)
        c.rect(1, 0, 14, 3, wood)
        c.rect(1, 2, 14, 3, gold)
        c.rect(3, 0, 4, 3, gold); c.rect(11, 0, 12, 3, gold)
        c.rect(1, 4, 14, 6, light)
        c.rect(0, 7, 15, 14, wood)
        c.rect(0, 7, 15, 8, gold)
        c.rect(3, 7, 4, 14, gold); c.rect(11, 7, 12, 14, gold)
        c.render()
    }

    // ---- UI glyphs -----------------------------------------------------------------------

    private fun glyph(key: String, rows: List<String>) = cached("glyph:$key") {
        val c = PixelCanvas(rows[0].length, rows.size)
        c.grid(rows, mapOf('#' to WHITE))
        c.render(outline = false)
    }

    val check get() = glyph("check", listOf(".......#", "......##", "#....##.", "##..##..", ".####...", "..##....").let { it })
    val plus get() = glyph("plus", listOf("..##..", "..##..", "######", "######", "..##..", "..##.."))
    val minus get() = glyph("minus", listOf("######", "######"))

    /** Bottom-bar icons, full colour. */
    fun navIcon(i: Int): Sprite = cached("nav:$i") {
        val c = PixelCanvas(12, 12)
        when (i) {
            0 -> { // quest board: a scroll with ticks
                c.grid(
                    listOf(
                        ".pppppppppp.",
                        "pPPPPPPPPPPp",
                        ".pppppppppp.",
                        ".pggpppppp..",
                        ".pggpkkkkp..",
                        ".pppppppppp.",
                        ".pggpppppp..",
                        ".pggpkkkkp..",
                        ".pppppppppp.",
                        ".pppppppppp.",
                        "pPPPPPPPPPPp",
                        ".pppppppppp.",
                    ),
                    mapOf('p' to mat(0xFFF1DDB0.toInt()), 'P' to mat(0xFFC99A5B.toInt()), 'g' to mat(0xFF4FC07A.toInt()), 'k' to Tone(Ramps.of(0xFFF1DDB0.toInt()), 1)),
                )
            }
            1 -> { // coin pouch
                c.grid(
                    listOf(
                        "....tttt....",
                        ".....tt.....",
                        "....bbbb....",
                        "...bbbbbb...",
                        "..bbbbbbbb..",
                        ".bbbbbbbbbb.",
                        ".bbbbccbbbb.",
                        ".bbbccccbbb.",
                        ".bbbbccbbbb.",
                        ".bbbbbbbbbb.",
                        "..bbbbbbbb..",
                        "............",
                    ),
                    mapOf('b' to mat(0xFFB0753F.toInt()), 't' to mat(0xFFE2435F.toInt()), 'c' to mat(0xFFF2C14E.toInt())),
                )
            }
            2 -> { // paw / egg
                c.ellipse(6f, 7f, 4.6f, 5f, mat(0xFFF6EBDD.toInt()))
                val spot = mat(0xFF4FC07A.toInt())
                listOf(5 to 4, 8 to 6, 4 to 8, 7 to 9, 6 to 6).forEach { (x, y) -> c.set(x, y, spot) }
            }
            3 -> { // boss skull
                c.grid(
                    listOf(
                        "...bbbbbb...",
                        "..bbbbbbbb..",
                        ".bbbbbbbbbb.",
                        ".bbbbbbbbbb.",
                        ".bKKKbbKKKb.",
                        ".bKKKbbKKKb.",
                        ".bbbbKKbbbb.",
                        "..bbbbbbbb..",
                        "...bkbkbkb..",
                        "...bbbbbbb..",
                        "............",
                        "............",
                    ),
                    mapOf('b' to mat(0xFFEDE6D6.toInt()), 'K' to Flat(0xFF2A1F38.toInt()), 'k' to Tone(Ramps.of(0xFFEDE6D6.toInt()), 1)),
                    dy = 1,
                )
            }
            else -> { // helmet
                c.grid(
                    listOf(
                        ".....rr.....",
                        "....rrrr....",
                        "...hhhhhh...",
                        "..hhhhhhhh..",
                        ".hhhhhhhhhh.",
                        ".hhhhhhhhhh.",
                        ".hhGGGGGGhh.",
                        ".hh......hh.",
                        ".hh.hhhh.hh.",
                        ".hh.hhhh.hh.",
                        ".hhhhhhhhhh.",
                        "..hhhhhhhh..",
                    ),
                    mapOf('h' to mat(0xFFB9C3CF.toInt()), 'G' to mat(0xFF3A3550.toInt()), 'r' to mat(0xFFE2435F.toInt())),
                )
            }
        }
        c.render()
    }

    // ---- Bosses --------------------------------------------------------------------------

    fun boss(shape: BossShape, primary: Int, secondary: Int, frame: Int = 0): Sprite = cached("boss:$shape:$primary:$frame") {
        val p = mat(primary)
        val s = mat(secondary)
        val glowS = mat(secondary, glows = true)
        when (shape) {
            BossShape.SLIME -> PixelCanvas(30, 26).apply {
                val squish = if (frame == 1) 0.6f else 0f
                ellipse(15f, 17f + squish, 13.5f + squish, 8.5f - squish, p)
                rect(3, 17, 26, 23, p)
                ellipse(15f, 23f, 12.5f, 2.2f, p)
                ellipse(9f, 12f, 2.5f, 1.6f, Tone(Ramps.of(primary), 4))
                // Crown
                grid(
                    listOf(
                        "..g...g...g..",
                        "..gg..g..gg..",
                        "..ggggggggg..",
                        "..gggrgrggg..",
                    ),
                    mapOf('g' to mat(secondary, glows = true), 'r' to Flat(0xFFE2435F.toInt())), dx = 8, dy = 3,
                )
                // Face
                listOf(10, 18).forEach { x ->
                    rect(x, 15, x + 2, 18, EYE); set(x, 15, WHITE)
                }
                rect(13, 20, 16, 20, EYE); set(12, 19, EYE); set(17, 19, EYE)
                set(6, 24, Tone(Ramps.of(primary), 1)); set(24, 24, Tone(Ramps.of(primary), 1))
            }.render()
            BossShape.GOBLIN -> PixelCanvas(28, 28).apply {
                val skin = mat(primary)
                // Club behind
                line(22, 4, 22, 22, mat(0xFF8A5A36.toInt()), 2)
                ellipse(23f, 5f, 3f, 4f, mat(0xFF8A5A36.toInt()))
                listOf(21 to 3, 25 to 5, 22 to 7).forEach { (x, y) -> set(x, y, mat(0xFFC9D2DC.toInt())) }
                // Body
                ellipse(13f, 21f, 8f, 6.5f, s)
                rect(6, 25, 9, 27, skin); rect(16, 25, 19, 27, skin)
                rect(5, 20, 21, 20, mat(0xFF4A2F1E.toInt()))
                ellipse(5f, 19f, 2.5f, 3.5f, skin); ellipse(21f, 18f, 2.5f, 3.5f, skin)
                // Head + ears
                tri(1f, 7f, 7f, 9f, 6f, 13f, skin); tri(25f, 7f, 19f, 9f, 20f, 13f, skin)
                ellipse(13f, 10.5f, 7f, 6.5f, skin)
                rect(8, 9, 11, 10, Flat(0xFFFFE066.toInt())); rect(15, 9, 18, 10, Flat(0xFFFFE066.toInt()))
                set(10, 10, EYE); set(16, 10, EYE)
                rect(9, 14, 17, 14, EYE); set(10, 13, WHITE); set(16, 13, WHITE)
                // Horned war helm
                grid(listOf("...hhhhhhh...", "..hhhhhhhhh..", ".h.........h."), mapOf('h' to mat(0xFF9AA6B4.toInt())), dx = 7, dy = 3)
            }.render()
            BossShape.GOLEM -> PixelCanvas(30, 28).apply {
                val rock = p
                val dark = Tone(Ramps.of(primary), 1)
                ellipse(5f, 15f + frame, 4f, 6f, rock); ellipse(25f, 15f + frame, 4f, 6f, rock)
                ellipse(5f, 22f + frame, 3.2f, 2.8f, rock); ellipse(25f, 22f + frame, 3.2f, 2.8f, rock)
                ellipse(15f, 17f, 9f, 8f, rock)
                rect(8, 23, 12, 27, rock); rect(18, 23, 22, 27, rock)
                ellipse(15f, 6.5f, 6f, 5f, rock)
                rect(11, 6, 13, 7, glowS); rect(17, 6, 19, 7, glowS)
                ellipse(15f, 17f, 2.5f, 2.5f, glowS)
                listOf(8 to 13, 21 to 20, 12 to 21, 19 to 12, 13 to 3).forEach { (x, y) -> set(x, y, dark); set(x + 1, y, dark) }
                // Moss
                listOf(9 to 10, 10 to 10, 20 to 10, 4 to 9, 26 to 9).forEach { (x, y) -> set(x, y, mat(0xFF6DBA5A.toInt())) }
            }.render()
            BossShape.WYRM -> PixelCanvas(32, 28).apply {
                val bob = frame
                // Body: a chain of discs rising in an S-curve
                val pts = listOf(4f to 25f, 8f to 25.5f, 12f to 24f, 15f to 21f, 16f to 17f, 15f to 13f, 16f to 10f, 19f to 8f)
                pts.forEachIndexed { k, (x, y) -> ellipse(x, y + if (k > 4) bob else 0, 3.6f, 3.4f, p) }
                // Belly scales
                pts.take(6).forEach { (x, y) -> set(x.toInt(), y.toInt() + 1, mat(secondary)) }
                // Fins
                tri(13f, 12f, 9f, 9f, 13f, 16f, s); tri(14f, 19f, 9f, 18f, 13f, 23f, s)
                // Head
                ellipse(22f, 8f + bob, 5f, 3.6f, p)
                tri(25f, 6.5f + bob, 31f, 8f + bob, 25f, 10.5f + bob, p)
                tri(19f, 5f + bob, 17f, 0f + bob, 22f, 5f + bob, mat(0xFFEAF6FF.toInt()))
                tri(23f, 5f + bob, 23f, 0f + bob, 26f, 5f + bob, mat(0xFFEAF6FF.toInt()))
                set(23, 7 + bob, Flat(0xFF3E5FBF.toInt())); set(23, 6 + bob, WHITE)
                // Frost breath
                listOf(31 to 10, 29 to 12, 31 to 13).forEach { (x, y) -> set(x, y + bob, mat(0xFFEAF6FF.toInt(), glows = true)) }
            }.render()
            BossShape.LICH -> PixelCanvas(28, 30).apply {
                val robe = p
                val bone = mat(0xFFE9E3D3.toInt())
                // Staff
                line(23, 6, 23, 29, mat(0xFF5A4636.toInt()), 1)
                ellipse(23.5f, 4.5f, 2.6f, 2.6f, glowS)
                // Robe
                tri(13f, 7f, 3f, 29f, 23f, 29f, robe)
                ellipse(13f, 11f, 7f, 7f, robe)
                rect(5, 27, 21, 29, robe)
                listOf(7, 11, 15, 19).forEach { x -> set(x, 29, null) }
                // Hood shadow and skull
                ellipse(13f, 12f, 4.5f, 4.5f, Flat(0xFF120E1E.toInt()))
                ellipse(13f, 12.5f, 3.4f, 3.2f, bone)
                set(12, 12, Flat(secondary)); set(14, 12, Flat(secondary))
                rect(12, 15, 14, 15, Tone(Ramps.of(0xFFE9E3D3.toInt()), 1))
                // Hands
                ellipse(21f, 17f, 1.8f, 1.8f, bone); ellipse(6f, 18f, 1.8f, 1.8f, bone)
                // Trim
                line(13, 18, 13, 28, mat(0xFF8E7CC3.toInt()))
                listOf(6 to 20, 3 to 23).forEach { (x, y) -> set(x, y - frame, glowS) }
            }.render()
            BossShape.DRAGON -> PixelCanvas(36, 30).apply {
                val body = p
                val wing = mat(Color.mix(primary, 0xFF3A1F3F.toInt(), 0.35f))
                val belly = mat(secondary)
                val flap = if (frame == 1) 3f else 0f
                // Wings
                tri(14f, 12f, 2f, 1f + flap, 6f, 16f, wing)
                tri(14f, 12f, 1f, 8f + flap, 7f, 18f, wing)
                tri(20f, 12f, 30f, 0f + flap, 28f, 15f, wing)
                // Tail
                listOf(6f to 25f, 3f to 23f, 1f to 20f).forEach { (x, y) -> ellipse(x, y, 2.4f, 2f, body) }
                tri(0f, 20f, 1f, 15f, 3f, 19f, belly)
                // Body
                ellipse(15f, 21f, 9f, 7f, body)
                ellipse(16f, 22f, 5f, 5f, belly)
                rect(9, 26, 12, 29, body); rect(18, 26, 21, 29, body)
                // Neck & head
                ellipse(22f, 14f, 3.2f, 4f, body)
                ellipse(26f, 9f, 5f, 4f, body)
                tri(29f, 7f, 35f, 9.5f, 29f, 12f, body)
                tri(23f, 6f, 21f, 0f, 26f, 6f, belly); tri(27f, 6f, 28f, 1f, 30f, 6f, belly)
                rect(26, 8, 27, 8, Flat(0xFFFFE066.toInt())); set(27, 8, EYE)
                set(33, 9, EYE)
                // Embers
                listOf(35 to 12, 33 to 14, 35 to 15).forEach { (x, y) -> set(x, y - frame, mat(0xFFFFB347.toInt(), glows = true)) }
            }.render()
            BossShape.YETI -> PixelCanvas(30, 30).apply {
                val fur = p
                val skin = s
                val bone = mat(0xFFF1E6D3.toInt())
                ellipse(4f, 17f + frame, 3.6f, 6.5f, fur); ellipse(26f, 17f + frame, 3.6f, 6.5f, fur)
                ellipse(4f, 23f + frame, 2.6f, 2f, skin); ellipse(26f, 23f + frame, 2.6f, 2f, skin)
                ellipse(15f, 19f, 10.5f, 9.5f, fur)
                rect(9, 26, 13, 29, fur); rect(17, 26, 21, 29, fur)
                ellipse(15f, 9f, 7f, 6.5f, fur)
                tri(9f, 5f, 6f, 0f, 11f, 4f, bone); tri(21f, 5f, 24f, 0f, 19f, 4f, bone)
                ellipse(15f, 10.5f, 4.6f, 3.6f, skin)
                set(13, 9, EYE); set(17, 9, EYE); set(13, 8, WHITE); set(17, 8, WHITE)
                rect(13, 12, 17, 12, Flat(0xFF3A2440.toInt())); set(13, 13, WHITE); set(17, 13, WHITE)
                ellipse(15f, 20f, 5f, 5.5f, Tone(Ramps.of(primary), 3))
                listOf(7 to 14, 23 to 15, 10 to 24, 20 to 23, 15 to 3).forEach { (x, y) -> set(x, y, Tone(Ramps.of(primary), 1)) }
            }.render()
            BossShape.TREANT -> PixelCanvas(32, 32).apply {
                val bark = p
                val bloom = mat(secondary)
                val leaf = mat(0xFF6DBA5A.toInt())
                // Roots and trunk
                tri(8f, 31f, 12f, 22f, 14f, 31f, bark); tri(24f, 31f, 20f, 22f, 18f, 31f, bark)
                rect(10, 12, 21, 29, bark)
                // Branch arms
                line(10, 16, 3, 10 + frame, bark, 2); line(21, 16, 28, 10 - frame, bark, 2)
                ellipse(3f, 9f + frame, 3f, 2.5f, leaf); ellipse(28f, 9f - frame, 3f, 2.5f, leaf)
                // Crown of blossoms
                ellipse(16f, 8f, 11f, 6.5f, leaf)
                listOf(8 to 6, 12 to 3, 17 to 4, 22 to 5, 25 to 9, 10 to 10, 19 to 9, 14 to 7).forEach { (x, y) -> ellipse(x + 0.5f, y + 0.5f, 1.6f, 1.6f, bloom) }
                // Face
                rect(12, 17, 14, 18, Flat(0xFF1E1530.toInt())); rect(18, 17, 20, 18, Flat(0xFF1E1530.toInt()))
                set(13, 17, mat(0xFF7CFFB2.toInt(), glows = true)); set(19, 17, mat(0xFF7CFFB2.toInt(), glows = true))
                rect(14, 22, 18, 23, Flat(0xFF1E1530.toInt()))
                listOf(11 to 26, 20 to 14, 15 to 28).forEach { (x, y) -> set(x, y, Tone(Ramps.of(primary), 1)) }
            }.render()
            BossShape.CRAB -> PixelCanvas(34, 26).apply {
                val shell = p
                val belly = s
                val leg = mat(Color.mix(primary, 0xFF3A1F3F.toInt(), 0.25f))
                line(12, 19, 5, 24, leg); line(14, 20, 9, 25, leg); line(20, 20, 25, 25, leg); line(22, 19, 29, 24, leg)
                ellipse(17f, 16f, 11f, 6.5f, shell)
                ellipse(17f, 19f, 7f, 2.5f, belly)
                // Eye stalks
                line(13, 10, 12, 5, shell); line(21, 10, 22, 5, shell)
                ellipse(12f, 4.5f, 1.8f, 1.8f, mat(0xFFFFFFFF.toInt())); ellipse(22f, 4.5f, 1.8f, 1.8f, mat(0xFFFFFFFF.toInt()))
                set(12, 4, EYE); set(22, 4, EYE)
                // Claws, one raised
                val lift = if (frame == 1) 2f else 0f
                ellipse(4f, 10f - lift, 4f, 3.4f, shell); tri(0f, 7f - lift, 5f, 9.5f - lift, 0f, 11f - lift, null)
                ellipse(30f, 12f, 4f, 3.4f, shell); tri(34f, 9f, 29f, 11.5f, 34f, 13f, null)
                line(7, 13, 10, 15, shell, 2); line(27, 15, 24, 16, shell, 2)
                rect(15, 14, 19, 14, Flat(0xFF3A1F2A.toInt()))
            }.render()
            BossShape.PUMPKIN -> PixelCanvas(30, 32).apply {
                val pumpkin = p
                val cape = mat(0xFF4B3F72.toInt())
                val dark = Tone(Ramps.of(primary), 1)
                // Cloak body
                tri(15f, 14f, 5f, 31f, 25f, 31f, cape)
                rect(6, 29, 24, 31, cape)
                line(8, 20, 2, 25 - frame, mat(0xFF7A5034.toInt()), 1); line(22, 20, 28, 25 - frame, mat(0xFF7A5034.toInt()), 1)
                // Pumpkin head with ridges
                ellipse(15f, 10f, 11f, 8f, pumpkin)
                listOf(9, 15, 21).forEach { x -> for (y in 4..16) if (this[x, y] != null) set(x, y, dark) }
                rect(14, 0, 15, 2, s); tri(16f, 1f, 21f, 0f, 18f, 3f, s)
                // Glowing carved face
                val glow = mat(0xFFFFE066.toInt(), glows = true)
                tri(8f, 9f, 12f, 9f, 10f, 6f, glow); tri(18f, 9f, 22f, 9f, 20f, 6f, glow)
                rect(9, 12, 21, 13, glow)
                listOf(10, 14, 18).forEach { x -> set(x, 12, pumpkin); set(x + 2, 13, pumpkin) }
            }.render()
            else -> MoreBosses.draw(shape, primary, secondary, frame)
        }
    }

    // ---- Achievements --------------------------------------------------------------------

    /** Bronze, silver, gold, crystal, legendary. */
    val medalColors = intArrayOf(0xFFC98A4B.toInt(), 0xFFC9D2DC.toInt(), 0xFFF2C14E.toInt(), 0xFF7FE3F0.toInt(), 0xFFB45CFF.toInt())

    fun medal(tier: Int, locked: Boolean = false): Sprite = cached("medal:$tier:$locked") {
        val c = PixelCanvas(12, 15)
        val col = if (locked) 0xFF6F6890.toInt() else medalColors[tier.coerceIn(0, 4)]
        c.tri(2f, 0f, 6f, 0f, 5f, 6f, mat(if (locked) 0xFF4A4466.toInt() else 0xFFE2435F.toInt()))
        c.tri(10f, 0f, 6f, 0f, 7f, 6f, mat(if (locked) 0xFF3A3556.toInt() else 0xFF4F7BD9.toInt()))
        c.ellipse(6f, 9.5f, 5f, 5f, mat(col, glows = !locked && tier >= 3))
        c.grid(listOf("..#..", ".###.", "#####", ".#.#."), mapOf('#' to Tone(Ramps.of(col), if (locked) 1 else 4)), dx = 4, dy = 7)
        c.render()
    }

    // ---- Home decor ----------------------------------------------------------------------

    private val animated = setOf("clock", "fireplace", "aquarium", "candles") + MoreDecor.animated

    fun decor(key: String, frame: Int = 0): Sprite = cached("decor:$key:${if (key in animated) frame else 0}") {
        val wood = mat(0xFF9A6334.toInt())
        val dark = mat(0xFF6B4630.toInt())
        val gold = mat(0xFFF2C14E.toInt())
        when (key) {
            "painting_hills", "painting_sea" -> PixelCanvas(18, 14).apply {
                rect(0, 0, 17, 13, if (key == "painting_hills") gold else wood)
                rect(1, 1, 16, 12, Flat(if (key == "painting_hills") 0xFF8CC8F5.toInt() else 0xFFFFC48A.toInt()))
                if (key == "painting_hills") {
                    ellipse(5f, 13f, 7f, 5f, Flat(0xFF6DBA5A.toInt())); ellipse(13f, 13f, 6f, 4f, Flat(0xFF4E9A4A.toInt()))
                    ellipse(13.5f, 4f, 1.6f, 1.6f, Flat(0xFFFFF4C2.toInt()))
                    rect(0, 13, 17, 13, gold)
                } else {
                    rect(1, 7, 16, 12, Flat(0xFF3E8FC9.toInt())); rect(1, 10, 16, 12, Flat(0xFF2A6FB0.toInt()))
                    tri(8f, 2f, 8f, 7f, 12f, 7f, Flat(0xFFFFFFFF.toInt())); rect(6, 7, 12, 7, Flat(0xFF7A5034.toInt()))
                    rect(0, 13, 17, 13, wood)
                }
            }
            "clock" -> PixelCanvas(10, 20).apply {
                rect(1, 0, 8, 19, wood)
                ellipse(5f, 5f, 3.4f, 3.4f, mat(0xFFF6EBDD.toInt()))
                set(4, 3, EYE); set(4, 4, EYE); set(5, 5, EYE); set(6, 5, EYE)
                rect(2, 10, 7, 18, Flat(0xFF3A2440.toInt()))
                val x = if (frame == 0) 3 else 6
                line(5, 10, x, 15, gold); ellipse(x + 0.5f, 16f, 1.4f, 1.4f, gold)
            }
            "map" -> PixelCanvas(20, 14).apply {
                rect(0, 0, 19, 13, mat(0xFFF1DDB0.toInt()))
                val ink = Flat(0xFF9A6B45.toInt())
                listOf(3 to 4, 5 to 5, 7 to 5, 9 to 6, 11 to 8, 13 to 8).forEach { (x, y) -> set(x, y, ink) }
                ellipse(5f, 9f, 3f, 2f, Flat(0xFF8BD36A.toInt())); ellipse(15f, 4f, 3f, 2f, Flat(0xFF8BD36A.toInt()))
                val r = Flat(0xFFE2435F.toInt())
                set(14, 7, r); set(16, 9, r); set(15, 8, r); set(16, 7, r); set(14, 9, r)
            }
            "banner" -> PixelCanvas(14, 22).apply {
                rect(0, 0, 13, 1, dark)
                rect(2, 2, 11, 20, mat(0xFF6A3FC8.toInt()))
                tri(2f, 21f, 7f, 16f, 12f, 21f, null)
                grid(listOf("..#..", ".###.", "#####", ".###.", ".#.#."), mapOf('#' to gold), dx = 5, dy = 6)
                rect(2, 3, 11, 3, gold)
            }
            "mirror" -> PixelCanvas(12, 18).apply {
                ellipse(6f, 9f, 6f, 9f, gold)
                ellipse(6f, 9f, 4.6f, 7.6f, Flat(0xFFCFE6F5.toInt()))
                line(4, 6, 7, 3, Flat(0xFFFFFFFF.toInt())); line(4, 9, 8, 5, Flat(0xFFFFFFFF.toInt()))
            }
            "window" -> PixelCanvas(20, 18).apply {
                // The glass stays empty: the room draws the real sky behind it.
                rect(0, 0, 19, 17, wood)
                rect(2, 2, 17, 15, null)
                rect(9, 2, 10, 15, wood); rect(2, 8, 17, 9, wood)
                rect(0, 16, 19, 17, mat(0xFFB07A45.toInt()))
            }
            "trophies" -> PixelCanvas(26, 13).apply {
                rect(0, 11, 25, 12, wood)
                listOf(2 to 0xFFC98A4B, 8 to 0xFFC9D2DC, 14 to 0xFFF2C14E, 20 to 0xFF7FE3F0).forEach { (x, col) ->
                    val m = mat(col.toInt(), glows = col == 0xFF7FE3F0)
                    ellipse(x + 2f, 3f, 2.6f, 2.6f, m); rect(x + 1, 3, x + 3, 7, m); rect(x, 8, x + 4, 10, m)
                }
            }
            "bed" -> PixelCanvas(30, 18).apply {
                rect(0, 0, 4, 17, wood); rect(25, 6, 29, 17, wood)
                rect(4, 8, 25, 13, mat(0xFFF6EBDD.toInt()))
                rect(10, 7, 25, 14, mat(0xFFE2435F.toInt()))
                ellipse(7f, 8f, 3f, 2f, mat(0xFFFFFFFF.toInt()))
                rect(4, 14, 25, 15, dark)
            }
            "desk" -> PixelCanvas(26, 16).apply {
                rect(0, 4, 25, 6, wood); rect(1, 7, 3, 15, wood); rect(22, 7, 24, 15, wood); rect(15, 7, 21, 11, dark)
                rect(4, 1, 9, 3, mat(0xFF4F7BD9.toInt())); rect(5, 0, 10, 0, mat(0xFFE2435F.toInt()))
                rect(19, 1, 19, 3, mat(0xFFF6EBDD.toInt())); set(19, 0, mat(0xFFFFC94A.toInt(), glows = true))
            }
            "bookshelf" -> PixelCanvas(20, 30).apply {
                rect(0, 0, 19, 29, wood)
                val spines = intArrayOf(0xFFE2435F.toInt(), 0xFF4F7BD9.toInt(), 0xFF4FC07A.toInt(), 0xFFF2C14E.toInt(), 0xFF9B82FF.toInt())
                listOf(2, 11, 20).forEach { y ->
                    rect(2, y, 17, y + 7, Flat(0xFF3A2440.toInt()))
                    var x = 2
                    var k = y
                    while (x < 17) {
                        val wdt = if (k % 3 == 0) 2 else 1
                        rect(x, y + 1 + k % 2, (x + wdt - 1).coerceAtMost(17), y + 7, mat(spines[k % spines.size]))
                        x += wdt + 1
                        k += 7
                    }
                }
            }
            "wardrobe" -> PixelCanvas(20, 30).apply {
                rect(0, 0, 19, 29, wood); rect(1, 2, 9, 27, mat(0xFFB07A45.toInt())); rect(10, 2, 18, 27, mat(0xFFB07A45.toInt()))
                set(8, 14, gold); set(11, 14, gold); rect(0, 28, 19, 29, dark)
            }
            "fireplace" -> PixelCanvas(26, 26).apply {
                val stone = mat(0xFF9AA0B5.toInt())
                rect(0, 4, 25, 25, stone); rect(0, 2, 25, 4, wood)
                ellipse(13f, 16f, 8f, 7f, Flat(0xFF1A1420.toInt())); rect(5, 16, 21, 25, Flat(0xFF1A1420.toInt()))
                listOf(2 to 8, 23 to 12, 3 to 22, 22 to 6).forEach { (x, y) -> set(x, y, Tone(Ramps.of(0xFF9AA0B5.toInt()), 1)) }
                val fire = mat(0xFFFF7A3D.toInt(), glows = true)
                val core = mat(0xFFFFE066.toInt(), glows = true)
                val h = if (frame == 0) 0f else 1.5f
                tri(8f, 25f, 12f, 25f, 10f, 17f - h, fire); tri(11f, 25f, 16f, 25f, 13.5f, 14f + h, fire); tri(15f, 25f, 19f, 25f, 17f, 18f - h, fire)
                tri(11f, 25f, 15f, 25f, 13f, 19f + h, core)
                rect(7, 24, 19, 25, wood)
            }
            "aquarium" -> PixelCanvas(26, 22).apply {
                rect(1, 14, 24, 21, wood)
                rect(0, 0, 25, 13, mat(0xFFCFE6F5.toInt()))
                rect(1, 3, 24, 12, Flat(0xFF3E8FC9.toInt())); rect(1, 10, 24, 12, Flat(0xFFE7C88E.toInt()))
                line(4, 10, 4, 5, mat(0xFF4FC07A.toInt())); line(6, 10, 7, 6, mat(0xFF4FC07A.toInt()))
                val fx = if (frame == 0) 12 else 16
                ellipse(fx + 0.5f, 6.5f, 2f, 1.4f, mat(0xFFFF7A3D.toInt())); set(fx - 2, 6, mat(0xFFFF7A3D.toInt())); set(fx + 1, 6, EYE)
                set(20, 4 + frame, Flat(0xFFFFFFFF.toInt())); set(19, 6 - frame, Flat(0xFFFFFFFF.toInt()))
            }
            "piano" -> PixelCanvas(30, 22).apply {
                val black = mat(0xFF2E2433.toInt())
                rect(0, 0, 29, 12, black); rect(0, 13, 29, 15, mat(0xFFF6EBDD.toInt()))
                for (x in 2..27 step 3) set(x, 13, EYE)
                rect(1, 16, 3, 21, black); rect(26, 16, 28, 21, black)
                rect(10, 2, 19, 6, mat(0xFFF6EBDD.toInt())); set(12, 3, EYE); set(15, 4, EYE); set(17, 3, EYE)
            }
            "plant" -> PixelCanvas(12, 16).apply {
                rect(3, 10, 8, 15, mat(0xFFC8693D.toInt())); rect(2, 10, 9, 11, mat(0xFFD9824F.toInt()))
                val l = mat(0xFF4FAE5A.toInt())
                ellipse(6f, 6f, 3f, 4f, l); ellipse(3f, 7f, 2.4f, 2f, l); ellipse(9f, 6f, 2.4f, 2.4f, l)
            }
            "barrel" -> PixelCanvas(11, 13).apply {
                ellipse(5.5f, 6.5f, 5f, 6.4f, wood); rect(1, 2, 9, 11, wood)
                rect(0, 3, 10, 3, mat(0xFF8E99A6.toInt())); rect(0, 9, 10, 9, mat(0xFF8E99A6.toInt()))
            }
            "candles" -> PixelCanvas(11, 11).apply {
                val wax = mat(0xFFF6EBDD.toInt())
                rect(1, 5, 2, 10, wax); rect(5, 3, 6, 10, wax); rect(8, 6, 9, 10, wax)
                val f = mat(0xFFFFC94A.toInt(), glows = true)
                set(1, 4 - frame, f); set(5, 2, f); set(5, 1 + frame, f); set(8, 5, f)
                rect(0, 10, 10, 10, gold)
            }
            "lamp" -> PixelCanvas(10, 20).apply {
                tri(5f, 0f, 0f, 7f, 10f, 7f, mat(0xFFFFE066.toInt(), glows = true))
                rect(4, 7, 5, 18, dark); rect(2, 18, 7, 19, dark)
            }
            "chest" -> PixelCanvas(14, 11).apply {
                ellipse(7f, 3.5f, 6.5f, 3.4f, wood); rect(0, 3, 13, 10, wood)
                rect(0, 4, 13, 4, gold); rect(6, 4, 7, 6, gold); rect(2, 0, 3, 10, gold); rect(10, 0, 11, 10, gold)
            }
            "pet_bed" -> PixelCanvas(18, 8).apply {
                ellipse(9f, 4.5f, 9f, 3.6f, mat(0xFF7C5CFF.toInt())); ellipse(9f, 4f, 6f, 2f, mat(0xFFCBBDFF.toInt()))
            }
            "globe" -> PixelCanvas(11, 15).apply {
                rect(5, 10, 5, 12, dark); rect(2, 13, 8, 14, dark)
                ellipse(5.5f, 5f, 4.6f, 4.6f, mat(0xFF4F9BEF.toInt()))
                ellipse(4f, 4f, 1.8f, 2.2f, Flat(0xFF4FC07A.toInt())); ellipse(7.5f, 6.5f, 1.4f, 1.2f, Flat(0xFF4FC07A.toInt()))
            }
            "guitar" -> PixelCanvas(10, 22).apply {
                rect(4, 0, 5, 11, dark); rect(3, 0, 6, 1, dark)
                ellipse(5f, 15f, 4.4f, 3.6f, mat(0xFFE8862E.toInt())); ellipse(5f, 11.5f, 3f, 2.6f, mat(0xFFE8862E.toInt()))
                ellipse(5f, 13.5f, 1.2f, 1.2f, Flat(0xFF3A2440.toInt())); rect(4, 18, 6, 18, dark)
            }
            "rug_red", "rug_blue", "rug_royal" -> PixelCanvas(54, 9).apply {
                val col = when (key) {
                    "rug_red" -> 0xFFC8364A.toInt()
                    "rug_blue" -> 0xFF3D6FD9.toInt()
                    else -> 0xFF6A3FC8.toInt()
                }
                val trim = if (key == "rug_royal") Flat(0xFFF2C14E.toInt()) else Flat(Color.mix(col, 0xFFFFFFFF.toInt(), 0.35f))
                rect(0, 0, 53, 8, Flat(Color.darken(col, 0.75f)))
                rect(2, 1, 51, 7, trim)
                rect(3, 2, 50, 6, Flat(col))
                for (x in 6..47 step 6) set(x, 4, trim)
            }
            "rug_round" -> PixelCanvas(44, 10).apply {
                ellipse(22f, 5f, 22f, 5f, Flat(0xFFE8C15A.toInt()))
                ellipse(22f, 5f, 17f, 3.6f, Flat(0xFF4FC07A.toInt()))
                ellipse(22f, 5f, 10f, 2f, Flat(0xFFF6EBDD.toInt()))
            }
            else -> MoreDecor.canvas(key, frame) ?: PixelCanvas(4, 4)
        }.render(outline = !key.startsWith("rug") && !key.startsWith("path") && key != "pond")
    }

    // ---- Scenery -------------------------------------------------------------------------

    fun tree(leaf: Int, variant: Int): Sprite = cached("tree:$leaf:$variant") {
        val c = PixelCanvas(16, 20)
        val trunk = mat(0xFF7A5034.toInt())
        c.rect(7, 11, 8, 19, trunk); c.set(6, 19, trunk); c.set(9, 19, trunk)
        val l = mat(leaf)
        if (variant % 2 == 0) {
            c.ellipse(8f, 7f, 6.5f, 5.5f, l); c.ellipse(4.5f, 10f, 3.8f, 3f, l); c.ellipse(11.5f, 10f, 3.8f, 3f, l)
        } else {
            c.ellipse(8f, 6f, 5f, 5f, l); c.ellipse(8f, 11f, 7f, 3.5f, l)
        }
        val ramp = Ramps.of(leaf)
        listOf(5 to 5, 10 to 4, 3 to 10, 12 to 9, 8 to 8).forEach { (x, y) -> c.set(x, y, Tone(ramp, if ((x + y) % 2 == 0) 3 else 1)) }
        c.render()
    }

    fun pine(leaf: Int, snow: Boolean): Sprite = cached("pine:$leaf:$snow") {
        val c = PixelCanvas(13, 22)
        c.rect(6, 18, 6, 21, mat(0xFF6B4630.toInt()))
        val l = mat(leaf)
        c.tri(6.5f, 0f, 1f, 8f, 12f, 8f, l)
        c.tri(6.5f, 4f, 0f, 13f, 13f, 13f, l)
        c.tri(6.5f, 9f, 0f, 19f, 13f, 19f, l)
        if (snow) {
            val sn = mat(0xFFF4F8FF.toInt())
            c.tri(6.5f, 0f, 4f, 4f, 9f, 4f, sn)
            c.rect(3, 8, 5, 8, sn); c.rect(8, 8, 10, 8, sn)
            c.rect(2, 13, 4, 13, sn); c.rect(9, 13, 11, 13, sn)
        }
        c.render()
    }

    fun bush(color: Int): Sprite = cached("bush:$color") {
        val c = PixelCanvas(12, 7)
        val l = mat(color)
        c.ellipse(4f, 4.5f, 3.6f, 3f, l); c.ellipse(8f, 4f, 3.8f, 3.4f, l)
        c.set(4, 3, Flat(0xFFF28DB2.toInt())); c.set(8, 2, Flat(0xFFFFFFFF.toInt()))
        c.render()
    }

    /** Autumn's patch of pumpkins: round, ridged, with a curly stem. */
    fun pumpkins(): Sprite = cached("pumpkins") {
        val c = PixelCanvas(16, 8)
        val big = mat(0xFFF08A2E.toInt())
        val ridge = Tone(Ramps.of(0xFFF08A2E.toInt()), 1)
        c.ellipse(5f, 5f, 4.6f, 3.2f, big)
        c.ellipse(12.5f, 6f, 3f, 2.2f, big)
        c.set(5, 3, ridge); c.set(5, 4, ridge); c.set(5, 5, ridge); c.set(12, 5, ridge)
        val stem = mat(0xFF5DAE4F.toInt())
        c.set(5, 1, stem); c.set(6, 0, stem); c.set(12, 3, stem)
        c.render()
    }

    fun snowdrift(): Sprite = cached("snowdrift") {
        val c = PixelCanvas(14, 5)
        val m = mat(0xFFF4F8FF.toInt())
        c.ellipse(5f, 4f, 5f, 3f, m); c.ellipse(10f, 4.5f, 4f, 2.2f, m)
        c.render()
    }

    fun rock(color: Int): Sprite = cached("rock:$color") {
        val c = PixelCanvas(9, 6)
        c.ellipse(4.5f, 4f, 4.3f, 3f, mat(color))
        c.rect(0, 5, 8, 5, mat(color))
        c.render()
    }

    fun cactus(): Sprite = cached("cactus") {
        val c = PixelCanvas(11, 16)
        val g = mat(0xFF4E9F5A.toInt())
        c.rect(4, 2, 6, 15, g); c.ellipse(5f, 2f, 1.6f, 1.6f, g)
        c.rect(1, 6, 2, 10, g); c.rect(1, 10, 4, 11, g)
        c.rect(8, 4, 9, 8, g); c.rect(6, 8, 9, 9, g)
        c.set(5, 0, Flat(0xFFF28DB2.toInt()))
        c.render()
    }

    fun palm(): Sprite = cached("palm") {
        val c = PixelCanvas(18, 24)
        val trunk = mat(0xFFB08452.toInt())
        listOf(9 to 23, 9 to 21, 10 to 19, 10 to 17, 10 to 15, 11 to 13, 11 to 11, 11 to 9).forEach { (x, y) -> c.rect(x - 1, y - 1, x, y, trunk) }
        val leaf = mat(0xFF4FAE5A.toInt())
        c.line(11, 7, 3, 9, leaf, 2); c.line(11, 7, 17, 10, leaf, 2); c.line(11, 7, 6, 2, leaf, 2)
        c.line(11, 7, 16, 3, leaf, 2); c.line(11, 7, 1, 5, leaf, 1)
        c.ellipse(11f, 9f, 1.4f, 1.4f, mat(0xFF7A5034.toInt()))
        c.render()
    }

    fun crystal(color: Int): Sprite = cached("crystal:$color") {
        val c = PixelCanvas(12, 14)
        val m = mat(color, glows = true)
        c.tri(6f, 0f, 3f, 13f, 9f, 13f, m)
        c.tri(2.5f, 5f, 0f, 13f, 5f, 13f, m)
        c.tri(9.5f, 6f, 7f, 13f, 12f, 13f, m)
        c.render()
    }

    fun cloud(variant: Int): Sprite = cached("cloud:$variant") {
        val c = PixelCanvas(24, 9)
        val m = Mat(Ramps.of(0xFFF7F4FF.toInt()), outlined = false)
        c.ellipse(7f, 6f, 6f, 3f, m); c.ellipse(13f, 4.5f, 6f, 4.5f, m); c.ellipse(18f, 6f, 5f, 3f, m)
        if (variant == 1) c.ellipse(9f, 3.5f, 3.5f, 3f, m)
        c.rect(2, 8, 22, 8, null)
        c.render(outline = false)
    }
}
