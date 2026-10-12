package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R

/** Places in the room. Each piece of furniture fits one kind of place. */
enum class Spot(val type: DecorType) {
    WALL_L(DecorType.WALL), WALL_C(DecorType.WALL), WALL_R(DecorType.WALL),
    BIG_L(DecorType.BIG), BIG_R(DecorType.BIG),
    SMALL_L(DecorType.SMALL), SMALL_R(DecorType.SMALL),
    RUG(DecorType.RUG),
}

enum class DecorType(@StringRes val label: Int) {
    WALL(R.string.decor_wall), BIG(R.string.decor_big), SMALL(R.string.decor_small), RUG(R.string.decor_rug),
    WALLPAPER(R.string.decor_wallpaper), FLOOR(R.string.decor_floor),
}

data class Decor(val key: String, val type: DecorType, val price: Double, @StringRes val name: Int, val level: Int = 1)

/** The hero's room: a gold sink that never runs dry and a place to make the game your own. */
object HomeCatalog {
    val items = listOf(
        Decor("painting_hills", DecorType.WALL, 60.0, R.string.d_painting_hills),
        Decor("painting_sea", DecorType.WALL, 90.0, R.string.d_painting_sea),
        Decor("clock", DecorType.WALL, 120.0, R.string.d_clock),
        Decor("map", DecorType.WALL, 140.0, R.string.d_map, 5),
        Decor("banner", DecorType.WALL, 150.0, R.string.d_banner, 8),
        Decor("mirror", DecorType.WALL, 180.0, R.string.d_mirror),
        Decor("window", DecorType.WALL, 200.0, R.string.d_window),
        Decor("trophies", DecorType.WALL, 250.0, R.string.d_trophies, 10),

        Decor("bed", DecorType.BIG, 180.0, R.string.d_bed),
        Decor("desk", DecorType.BIG, 160.0, R.string.d_desk),
        Decor("bookshelf", DecorType.BIG, 220.0, R.string.d_bookshelf),
        Decor("wardrobe", DecorType.BIG, 260.0, R.string.d_wardrobe),
        Decor("fireplace", DecorType.BIG, 450.0, R.string.d_fireplace, 12),
        Decor("aquarium", DecorType.BIG, 600.0, R.string.d_aquarium, 20),
        Decor("piano", DecorType.BIG, 900.0, R.string.d_piano, 30),

        Decor("plant", DecorType.SMALL, 40.0, R.string.d_plant),
        Decor("barrel", DecorType.SMALL, 50.0, R.string.d_barrel),
        Decor("candles", DecorType.SMALL, 60.0, R.string.d_candles),
        Decor("lamp", DecorType.SMALL, 70.0, R.string.d_lamp),
        Decor("chest", DecorType.SMALL, 90.0, R.string.d_chest),
        Decor("pet_bed", DecorType.SMALL, 110.0, R.string.d_pet_bed),
        Decor("globe", DecorType.SMALL, 120.0, R.string.d_globe, 10),
        Decor("guitar", DecorType.SMALL, 150.0, R.string.d_guitar, 15),

        Decor("rug_red", DecorType.RUG, 80.0, R.string.d_rug_red),
        Decor("rug_blue", DecorType.RUG, 80.0, R.string.d_rug_blue),
        Decor("rug_round", DecorType.RUG, 140.0, R.string.d_rug_round),
        Decor("rug_royal", DecorType.RUG, 350.0, R.string.d_rug_royal, 25),

        Decor("wp_plain", DecorType.WALLPAPER, 0.0, R.string.d_wp_plain),
        Decor("wp_stripes", DecorType.WALLPAPER, 120.0, R.string.d_wp_stripes),
        Decor("wp_dots", DecorType.WALLPAPER, 120.0, R.string.d_wp_dots),
        Decor("wp_bricks", DecorType.WALLPAPER, 160.0, R.string.d_wp_bricks),
        Decor("wp_wood", DecorType.WALLPAPER, 200.0, R.string.d_wp_wood),
        Decor("wp_stars", DecorType.WALLPAPER, 350.0, R.string.d_wp_stars, 15),

        Decor("fl_planks", DecorType.FLOOR, 0.0, R.string.d_fl_planks),
        Decor("fl_tiles", DecorType.FLOOR, 120.0, R.string.d_fl_tiles),
        Decor("fl_stone", DecorType.FLOOR, 160.0, R.string.d_fl_stone),
        Decor("fl_carpet", DecorType.FLOOR, 200.0, R.string.d_fl_carpet),
        Decor("fl_checker", DecorType.FLOOR, 260.0, R.string.d_fl_checker, 10),
    )

    fun item(key: String) = items.firstOrNull { it.key == key }

    /** "WALL_L=clock,RUG=rug_red" ⇄ map. */
    fun placement(csv: String): Map<Spot, String> = csv.split(',').mapNotNull { e ->
        val (s, k) = e.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        (runCatching { Spot.valueOf(s) }.getOrNull() ?: return@mapNotNull null) to k
    }.toMap()

    fun encode(m: Map<Spot, String>) = m.entries.joinToString(",") { "${it.key.name}=${it.value}" }
}
