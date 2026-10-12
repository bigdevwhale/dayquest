package app.dayquest

import app.dayquest.game.Catalog
import app.dayquest.game.Food
import app.dayquest.game.Potion
import app.dayquest.game.SceneKind
import app.dayquest.game.Species
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
        sheet("bosses", Catalog.bosses.map { Sprites.boss(it.shape, it.primary, it.secondary) }, scale = 6, cols = 3)
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
        )
        val scale = 4
        val r = SceneRenderer(180, 72)
        val img = Img(180 * scale, 72 * scale * frames.size)
        frames.forEachIndexed { i, s ->
            r.render(s, 5_000L + i * 1000)
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

        val seasonalBosses = app.dayquest.game.Season.entries.map { s -> Catalog.seasonalBoss(s, 2026, 10) }
        val ranked = (0..3).map { r -> Catalog.nextFight(Catalog.bosses[0], r) } + (0..3).map { r -> Catalog.nextFight(Catalog.bosses[5], r) }
        val weekly = (40..43).map { w -> Catalog.weekly(2026, w, 10) }
        sheet("bosses2", (seasonalBosses + ranked + weekly).map { Sprites.boss(it.shape, it.primary, it.secondary) }, scale = 5, cols = 4)

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
