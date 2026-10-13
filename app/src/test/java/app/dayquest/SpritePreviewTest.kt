package app.dayquest

import app.dayquest.game.Catalog
import app.dayquest.game.Food
import app.dayquest.game.Potion
import app.dayquest.game.SceneKind
import app.dayquest.game.Species
import app.dayquest.game.Spot as S
import app.dayquest.pixel.Look
import app.dayquest.pixel.SceneRenderer
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprite
import app.dayquest.pixel.Sprites
import org.junit.Test
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

/**
 * Builds every sprite (catches ragged grids and missing inks) and, when `-Ppreview=<dir>`
 * passes the system property, writes contact sheets to look at.
 */
class SpritePreviewTest {
    private val out = System.getProperty("preview.dir")?.let(::File)

    private fun sheet(name: String, sprites: List<Sprite>, scale: Int = 6, cols: Int = 8, bg: Int = 0xFF1C1930.toInt()) {
        val dir = out ?: return
        dir.mkdirs()
        val cw = sprites.maxOf { it.w } + 2
        val ch = sprites.maxOf { it.h } + 2
        val rows = (sprites.size + cols - 1) / cols
        val img = Img(cw * cols * scale, ch * rows * scale)
        for (y in 0 until img.height) for (x in 0 until img.width) img.setRGB(x, y, bg)
        sprites.forEachIndexed { i, s ->
            val ox = (i % cols) * cw + 1
            val oy = (i / cols) * ch + 1
            for (y in 0 until s.h) for (x in 0 until s.w) {
                val c = s.at(x, y)
                if (c ushr 24 == 0) continue
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB((ox + x) * scale + dx, (oy + y) * scale + dy, c)
            }
        }
        img.save(File(dir, "$name.png"))
    }

    @Test
    fun heroes() {
        val looks = (0 until 6).map { i ->
            Look(i, i, i + 1, Catalog.fixedGear.filter { it.slot.name == "WEAPON" }[i % 7].key, Catalog.fixedGear.filter { it.slot.name == "ARMOR" }[i].key,
                listOf("", "cap", "iron_helm", "wizard_hat", "horned_helm", "crown")[i], listOf("", "buckler", "kite_shield", "spell_tome", "aegis", "")[i])
        }
        sheet("heroes", looks.map { Sprites.hero(it) } + looks.take(2).map { Sprites.hero(it, blink = true) }, scale = 8, cols = 8)
    }

    @Test
    fun newGear() {
        val looks = listOf(
            Look(1, 0, 2, "dagger", "ranger_cloak", "hood", "tower_shield"),
            Look(2, 1, 4, "spear", "sunsteel", "bandana", "wisp_lantern"),
            Look(3, 2, 6, "willow_wand", "shadow_cloak", "feather_cap", "crystal_orb"),
            Look(0, 3, 1, "war_hammer", "archmage_robe", "circlet", ""),
            Look(4, 4, 3, "elven_bow", "leather", "", "buckler"),
            Look(5, 5, 7, "moon_scythe", "chainmail", "crown", ""),
            Look(1, 1, 8, "sun_hammer", "knight_plate", "iron_helm", "aegis"),
            Look(2, 0, 0, "blossom_wand", "petal_cloak", "flower_crown", ""),
            Look(3, 1, 3, "coral_trident", "island_shirt", "sun_hat", ""),
            Look(1, 2, 2, "harvest_scythe", "leaf_cloak", "pumpkin_hat", ""),
            Look(0, 4, 5, "snowflake_staff", "fur_cloak", "winter_hat", "wisp_lantern"),
        )
        sheet("gear_new", looks.map { Sprites.hero(it) }, scale = 8, cols = 6)
    }

    @Test
    fun pets() {
        val all = Species.entries.flatMap { s -> listOf(Sprites.pet(s, Potion.BASE), Sprites.pet(s, Potion.GOLDEN, grown = true), Sprites.pet(s, Potion.FROST), Sprites.pet(s, Potion.SHADOW)) }
        sheet("pets", all, scale = 8, cols = 8)
    }

    @Test
    fun items() {
        val list = Species.entries.map { Sprites.egg(it) } + Potion.entries.map { Sprites.potion(it) } + Food.entries.map { Sprites.food(it) } +
            listOf(Sprites.coin, Sprites.heart, Sprites.star, Sprites.gem, Sprites.healthPotion, Sprites.tombstone, Sprites.chest) +
            (0..4).map { Sprites.navIcon(it) } + listOf(Sprites.check, Sprites.plus, Sprites.minus)
        sheet("items", list, scale = 8, cols = 10)
    }

    @Test
    fun bosses() {
        sheet("bosses", Catalog.bosses.map { Sprites.boss(it.shape, it.primary, it.secondary) }, scale = 6, cols = 4)
        sheet("bosses_frame1", Catalog.bosses.map { Sprites.boss(it.shape, it.primary, it.secondary, 1) }, scale = 6, cols = 4)
    }

    @Test
    fun mountsAndBadges() {
        val mounts = Species.entries.flatMap { s -> listOf(Sprites.mount(s, Potion.BASE), Sprites.mount(s, Potion.FROST)) }
        sheet("mounts", mounts, scale = 6, cols = 8)
        val badges = app.dayquest.game.HeroClass.entries.map { app.dayquest.pixel.Badges.emblem(it) } +
            (1..5).map { app.dayquest.pixel.Badges.mood(it) } +
            listOf(app.dayquest.pixel.Badges.mana, app.dayquest.pixel.Badges.saddle, app.dayquest.pixel.Badges.hourglass, app.dayquest.pixel.Badges.mug, app.dayquest.pixel.Badges.door)
        sheet("badges", badges, scale = 10, cols = 7)
        val dir = out ?: return
        val look = Look(1, 0, 2, "flame_blade", "chainmail", "wizard_hat", "kite_shield")
        val frames = listOf(
            SceneState(SceneKind.MEADOW, 12f, look, "fox:base", mount = "wolf:base"),
            SceneState(SceneKind.FOREST, 14f, look, "owl:base", boss = Catalog.boss("goblin_chief"), mount = "dragon:ember"),
            SceneState(SceneKind.SNOW, 10f, look, "", mount = "turtle:frost"),
        )
        val scale = 4
        val r = SceneRenderer(180, 72)
        val img = Img(180 * scale, 72 * scale * frames.size)
        frames.forEachIndexed { i, s ->
            repeat(30) { k -> r.render(s, 5_000L + i * 1000 + k * 70) }
            for (y in 0 until r.h) for (x in 0 until r.w) {
                val c = r.buffer.px[y * r.w + x] or (0xFF shl 24)
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(x * scale + dx, (i * r.h + y) * scale + dy, c)
            }
        }
        dir.mkdirs()
        img.save(File(dir, "mount_scenes.png"))
    }

    @Test
    fun scenes() {
        val dir = out ?: return
        val look = Look(1, 0, 2, "flame_blade", "chainmail", "wizard_hat", "kite_shield")
        val frames = listOf(
            SceneState(SceneKind.MEADOW, 12f, look, "fox:base"),
            SceneState(SceneKind.MEADOW, 19f, look, "fox:golden", true),
            SceneState(SceneKind.FOREST, 23f, look, "owl:base", boss = Catalog.boss("shadow_lich")),
            SceneState(SceneKind.SNOW, 10f, look, "wolf:frost", boss = Catalog.boss("frost_wyrm")),
            SceneState(SceneKind.SHORE, 6.5f, look, "turtle:base"),
            SceneState(SceneKind.DESERT, 15f, look, "dragon:ember", boss = Catalog.boss("ember_dragon")),
            SceneState(SceneKind.CAVE, 12f, look, "slime:rose", boss = Catalog.boss("stone_golem")),
            SceneState(SceneKind.MEADOW, 9f, look, "cat:base", boss = Catalog.boss("slime_king")),
            SceneState(SceneKind.FOREST, 14f, look, "bunny:base", boss = Catalog.boss("goblin_chief")),
            SceneState(SceneKind.GROVE, 11f, look, "fox:blossom"),
            SceneState(SceneKind.CASTLE, 17.8f, look, "wolf:base", boss = Catalog.bosses.last()),
            SceneState(SceneKind.VOLCANO, 13f, look, "dragon:base", boss = Catalog.boss("ember_dragon")),
            SceneState(SceneKind.SKY, 10f, look, "owl:aurora"),
            SceneState(SceneKind.GROVE, 22f, look, "cat:base"),
        )
        val scale = 4
        val r = SceneRenderer(180, 72)
        val img = Img(180 * scale, 72 * scale * frames.size)
        frames.forEachIndexed { i, s ->
            repeat(30) { k -> r.render(s, 5_000L + i * 1000 + k * 70) }
            for (y in 0 until r.h) for (x in 0 until r.w) {
                val c = r.buffer.px[y * r.w + x] or (0xFF shl 24)
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(x * scale + dx, (i * r.h + y) * scale + dy, c)
            }
        }
        dir.mkdirs()
        img.save(File(dir, "scenes.png"))
    }

    @Test
    fun longGame() {
        val base = Look(2, 1, 3, "iron_sword", "leather", "", "buckler")
        val hats = listOf("flower_crown", "sun_hat", "pumpkin_hat", "winter_hat").map { Sprites.hero(base.copy(head = it)) }
        val forged = (1..4).map { t -> Sprites.hero(Look(1, 0, 2, app.dayquest.game.Forge.key(t, app.dayquest.game.Slot.WEAPON), app.dayquest.game.Forge.key(t, app.dayquest.game.Slot.ARMOR), app.dayquest.game.Forge.key(t, app.dayquest.game.Slot.HEAD), app.dayquest.game.Forge.key(t, app.dayquest.game.Slot.SHIELD))) }
        sheet("hats_forge", hats + forged, scale = 8, cols = 8)

        val seasonalBosses = app.dayquest.game.Season.entries.flatMap { s -> Catalog.seasonalBosses(s, 2026, 10) }
        val ranked = (0..3).map { r -> Catalog.nextFight(Catalog.bosses[0], r) } + (0..3).map { r -> Catalog.nextFight(Catalog.bosses[5], r) }
        val weekly = (40..43).map { w -> Catalog.weekly(2026, w, 10) }
        sheet("bosses2", (seasonalBosses + ranked + weekly).map { Sprites.boss(it.shape, it.primary, it.secondary) }, scale = 5, cols = 6)

        val seasonalPets = Species.entries.flatMap { s -> listOf(Potion.BLOSSOM, Potion.TIDE, Potion.HARVEST, Potion.AURORA).map { Sprites.pet(s, it) } }
        sheet("pets2", seasonalPets, scale = 7, cols = 8)

        val medals = (0..4).map { Sprites.medal(it) } + Sprites.medal(0, locked = true)
        val decor = app.dayquest.game.HomeCatalog.items.filter { it.type.name !in setOf("WALLPAPER", "FLOOR") }.map { Sprites.decor(it.key) }
        sheet("decor", medals + decor, scale = 5, cols = 8)
    }

    @Test
    fun rooms() {
        val dir = out ?: return
        val look = Look(1, 3, 4, "flame_blade", "chainmail", "wizard_hat", "kite_shield")
        val full = mapOf(
            app.dayquest.game.Spot.WALL_L to "painting_hills", app.dayquest.game.Spot.WALL_C to "window", app.dayquest.game.Spot.WALL_R to "trophies",
            app.dayquest.game.Spot.BIG_L to "bookshelf", app.dayquest.game.Spot.BIG_R to "fireplace",
            app.dayquest.game.Spot.SMALL_L to "plant", app.dayquest.game.Spot.SMALL_R to "pet_bed", app.dayquest.game.Spot.RUG to "rug_royal",
        )
        val states = listOf(
            app.dayquest.pixel.RoomState(look, "fox:golden", true, emptyMap(), hour = 12f),
            app.dayquest.pixel.RoomState(look, "fox:golden", true, full, "wp_bricks", "fl_planks", 12f),
            app.dayquest.pixel.RoomState(look, "cat:base", false, full + (app.dayquest.game.Spot.BIG_L to "aquarium") + (app.dayquest.game.Spot.SMALL_L to "lamp"), "wp_stars", "fl_checker", 22.5f),
            app.dayquest.pixel.RoomState(look, "owl:aurora", false, full + (app.dayquest.game.Spot.BIG_R to "piano") + (app.dayquest.game.Spot.WALL_L to "clock"), "wp_wood", "fl_tiles", 18.5f),
        )
        val scale = 4
        val r = app.dayquest.pixel.RoomRenderer(176, 84)
        val img = Img(r.w * scale, r.h * scale * states.size)
        states.forEachIndexed { i, s ->
            r.render(s, 5_000L + i * 777)
            for (y in 0 until r.h) for (x in 0 until r.w) {
                val c = r.buffer.px[y * r.w + x] or (0xFF shl 24)
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(x * scale + dx, (i * r.h + y) * scale + dy, c)
            }
        }
        dir.mkdirs()
        img.save(File(dir, "rooms.png"))
    }

    @Test
    fun gardenAndAttic() {
        val dir = out ?: return
        val look = Look(1, 3, 4, "elven_bow", "ranger_cloak", "feather_cap", "")

        val garden = mapOf(
            S.G_BIG_L to "apple_tree", S.G_BIG_R to "well", S.G_SMALL_L to "flower_bed", S.G_SMALL_C to "lamp_post", S.G_SMALL_R to "gnome",
            S.G_FENCE to "fence_picket", S.G_PATH to "path_stone",
        )
        val garden2 = mapOf(
            S.G_BIG_L to "fountain", S.G_BIG_R to "windmill", S.G_SMALL_L to "beehive", S.G_SMALL_C to "bench", S.G_SMALL_R to "pumpkin_patch",
            S.G_FENCE to "fence_hedge", S.G_PATH to "path_brick",
        )
        val garden3 = mapOf(
            S.G_BIG_L to "pond", S.G_BIG_R to "garden_shed", S.G_SMALL_L to "mailbox", S.G_SMALL_C to "garden_scarecrow", S.G_SMALL_R to "birdhouse",
            S.G_FENCE to "fence_stone", S.G_PATH to "path_dirt",
        )
        val scale = 4
        val r = SceneRenderer(176, 84, ground = 30)
        val frames = listOf(
            SceneState(SceneKind.MEADOW, 11f, look, "fox:base", season = app.dayquest.game.Season.SUMMER, garden = garden),
            SceneState(SceneKind.MEADOW, 21.5f, look, "fox:base", season = app.dayquest.game.Season.AUTUMN, garden = garden),
            SceneState(SceneKind.MEADOW, 15f, look, "cat:aurora", season = app.dayquest.game.Season.WINTER, garden = garden2),
            SceneState(SceneKind.MEADOW, 9f, look, "bunny:blossom", season = app.dayquest.game.Season.SPRING, garden = garden3),
        )
        val img = Img(r.w * scale, r.h * scale * (frames.size + 2))
        frames.forEachIndexed { i, s ->
            repeat(20) { k -> r.render(s, 3_000L + i * 4000 + k * 70) }
            for (y in 0 until r.h) for (x in 0 until r.w) {
                val c = r.buffer.px[y * r.w + x] or (0xFF shl 24)
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(x * scale + dx, (i * r.h + y) * scale + dy, c)
            }
        }
        val room = app.dayquest.pixel.RoomRenderer(176, 84)
        val attic = mapOf(
            S.WALL_L to "weapon_rack", S.WALL_R to "plant_shelf", S.BIG_L to "alchemy_table", S.BIG_R to "armor_stand",
            S.SMALL_L to "cauldron", S.SMALL_R to "crystal_ball", S.RUG to "rug_star",
        )
        listOf(
            app.dayquest.pixel.RoomState(look, "owl:base", false, attic, "wp_castle", "fl_parquet", 12f, attic = true),
            app.dayquest.pixel.RoomState(look, "owl:base", false, attic + (S.BIG_R to "sofa") + (S.SMALL_L to "books") + (S.RUG to "rug_fur"), "wp_leaves", "fl_marble", 22f, attic = true),
        ).forEachIndexed { j, st ->
            room.render(st, 5_000L)
            val i = frames.size + j
            for (y in 0 until room.h) for (x in 0 until room.w) {
                val c = room.buffer.px[y * room.w + x] or (0xFF shl 24)
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(x * scale + dx, (i * room.h + y) * scale + dy, c)
            }
        }
        dir.mkdirs()
        img.save(File(dir, "garden.png"))
    }

    @Test
    fun seasons() {
        val dir = out ?: return
        val look = Look(1, 0, 2, "flame_blade", "chainmail", "flower_crown", "kite_shield")
        val frames = app.dayquest.game.Season.entries.flatMap { s ->
            listOf(SceneState(SceneKind.MEADOW, 11f, look, "fox:blossom", season = s), SceneState(SceneKind.FOREST, 15f, look, "wolf:aurora", season = s))
        }
        val scale = 3
        val r = SceneRenderer(180, 72)
        val img = Img(180 * scale, 72 * scale * frames.size)
        frames.forEachIndexed { i, s ->
            repeat(40) { k -> r.render(s, 1_000L + i * 5000 + k * 70) }
            for (y in 0 until r.h) for (x in 0 until r.w) {
                val c = r.buffer.px[y * r.w + x] or (0xFF shl 24)
                for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(x * scale + dx, (i * r.h + y) * scale + dy, c)
            }
        }
        dir.mkdirs()
        img.save(File(dir, "seasons.png"))
    }
}

/** Minimal RGBA PNG writer: Android unit tests have no java.awt. */
private class Img(val width: Int, val height: Int) {
    private val px = IntArray(width * height)
    fun setRGB(x: Int, y: Int, c: Int) { px[y * width + x] = c }

    fun save(f: File) {
        val raw = ByteArrayOutputStream()
        for (y in 0 until height) {
            raw.write(0)
            for (x in 0 until width) {
                val c = px[y * width + x]
                raw.write(c shr 16 and 0xFF); raw.write(c shr 8 and 0xFF); raw.write(c and 0xFF); raw.write(c ushr 24)
            }
        }
        val z = ByteArrayOutputStream().also { DeflaterOutputStream(it).use { d -> d.write(raw.toByteArray()) } }.toByteArray()
        val out = DataOutputStream(f.outputStream())
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        fun chunk(type: String, data: ByteArray) {
            out.writeInt(data.size)
            val t = type.toByteArray()
            out.write(t); out.write(data)
            out.writeInt(CRC32().apply { update(t); update(data) }.value.toInt())
        }
        val ihdr = ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(width); writeInt(height); write(8); write(6); write(0); write(0); write(0) } }.toByteArray()
        chunk("IHDR", ihdr); chunk("IDAT", z); chunk("IEND", ByteArray(0))
        out.close()
    }
}
