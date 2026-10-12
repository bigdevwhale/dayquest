package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R

/** The parts of the hero's home: the room, the attic above it and the garden outside. */
enum class HomeArea(val key: String, val price: Double, val level: Int, @StringRes val label: Int) {
    ROOM("room", 0.0, 1, R.string.area_room),
    GARDEN("garden", 250.0, 5, R.string.area_garden),
    ATTIC("attic", 600.0, 12, R.string.area_attic);

    /** Bought areas are recorded among owned decor under this key. */
    val ownedKey get() = "area:$key"
}

/** Places in the home. Each piece of furniture fits one kind of place. */
enum class Spot(val type: DecorType, val area: HomeArea = HomeArea.ROOM) {
    WALL_L(DecorType.WALL), WALL_C(DecorType.WALL), WALL_R(DecorType.WALL),
    BIG_L(DecorType.BIG), BIG_R(DecorType.BIG),
    SMALL_L(DecorType.SMALL), SMALL_R(DecorType.SMALL),
    RUG(DecorType.RUG),

    A_WALL_L(DecorType.WALL, HomeArea.ATTIC), A_WALL_R(DecorType.WALL, HomeArea.ATTIC),
    A_BIG_L(DecorType.BIG, HomeArea.ATTIC), A_BIG_R(DecorType.BIG, HomeArea.ATTIC),
    A_SMALL_L(DecorType.SMALL, HomeArea.ATTIC), A_SMALL_R(DecorType.SMALL, HomeArea.ATTIC),
    A_RUG(DecorType.RUG, HomeArea.ATTIC),
    A_WALLPAPER(DecorType.WALLPAPER, HomeArea.ATTIC), A_FLOOR(DecorType.FLOOR, HomeArea.ATTIC),

    G_BIG_L(DecorType.GARDEN_BIG, HomeArea.GARDEN), G_BIG_R(DecorType.GARDEN_BIG, HomeArea.GARDEN),
    G_SMALL_L(DecorType.GARDEN_SMALL, HomeArea.GARDEN), G_SMALL_C(DecorType.GARDEN_SMALL, HomeArea.GARDEN), G_SMALL_R(DecorType.GARDEN_SMALL, HomeArea.GARDEN),
    G_FENCE(DecorType.FENCE, HomeArea.GARDEN), G_PATH(DecorType.PATH, HomeArea.GARDEN);

    /** The attic's spots stand in for the room's when the attic is drawn. */
    val roomTwin: Spot? get() = when (this) {
        A_WALL_L -> WALL_L
        A_WALL_R -> WALL_R
        A_BIG_L -> BIG_L
        A_BIG_R -> BIG_R
        A_SMALL_L -> SMALL_L
        A_SMALL_R -> SMALL_R
        A_RUG -> RUG
        else -> null
    }
}

enum class DecorType(@StringRes val label: Int) {
    WALL(R.string.decor_wall), BIG(R.string.decor_big), SMALL(R.string.decor_small), RUG(R.string.decor_rug),
    WALLPAPER(R.string.decor_wallpaper), FLOOR(R.string.decor_floor),
    GARDEN_BIG(R.string.decor_garden_big), GARDEN_SMALL(R.string.decor_garden_small), FENCE(R.string.decor_fence), PATH(R.string.decor_path),
}

data class Decor(val key: String, val type: DecorType, val price: Double, @StringRes val name: Int, val level: Int = 1)

/** The hero's home: a gold sink that never runs dry and a place to make the game your own. */
object HomeCatalog {
    val items = listOf(
        Decor("painting_hills", DecorType.WALL, 60.0, R.string.d_painting_hills),
        Decor("painting_sea", DecorType.WALL, 90.0, R.string.d_painting_sea),
        Decor("clock", DecorType.WALL, 120.0, R.string.d_clock),
        Decor("plant_shelf", DecorType.WALL, 130.0, R.string.d_plant_shelf),
        Decor("map", DecorType.WALL, 140.0, R.string.d_map, 5),
        Decor("banner", DecorType.WALL, 150.0, R.string.d_banner, 8),
        Decor("mirror", DecorType.WALL, 180.0, R.string.d_mirror),
        Decor("window", DecorType.WALL, 200.0, R.string.d_window),
        Decor("weapon_rack", DecorType.WALL, 220.0, R.string.d_weapon_rack, 8),
        Decor("trophies", DecorType.WALL, 250.0, R.string.d_trophies, 10),

        Decor("bed", DecorType.BIG, 180.0, R.string.d_bed),
        Decor("desk", DecorType.BIG, 160.0, R.string.d_desk),
        Decor("bookshelf", DecorType.BIG, 220.0, R.string.d_bookshelf),
        Decor("sofa", DecorType.BIG, 240.0, R.string.d_sofa),
        Decor("wardrobe", DecorType.BIG, 260.0, R.string.d_wardrobe),
        Decor("armor_stand", DecorType.BIG, 350.0, R.string.d_armor_stand, 14),
        Decor("fireplace", DecorType.BIG, 450.0, R.string.d_fireplace, 12),
        Decor("alchemy_table", DecorType.BIG, 500.0, R.string.d_alchemy_table, 18),
        Decor("aquarium", DecorType.BIG, 600.0, R.string.d_aquarium, 20),
        Decor("piano", DecorType.BIG, 900.0, R.string.d_piano, 30),

        Decor("plant", DecorType.SMALL, 40.0, R.string.d_plant),
        Decor("books", DecorType.SMALL, 45.0, R.string.d_books),
        Decor("barrel", DecorType.SMALL, 50.0, R.string.d_barrel),
        Decor("candles", DecorType.SMALL, 60.0, R.string.d_candles),
        Decor("lamp", DecorType.SMALL, 70.0, R.string.d_lamp),
        Decor("chest", DecorType.SMALL, 90.0, R.string.d_chest),
        Decor("pet_bed", DecorType.SMALL, 110.0, R.string.d_pet_bed),
        Decor("globe", DecorType.SMALL, 120.0, R.string.d_globe, 10),
        Decor("guitar", DecorType.SMALL, 150.0, R.string.d_guitar, 15),
        Decor("cauldron", DecorType.SMALL, 180.0, R.string.d_cauldron, 9),
        Decor("crystal_ball", DecorType.SMALL, 220.0, R.string.d_crystal_ball, 12),

        Decor("rug_red", DecorType.RUG, 80.0, R.string.d_rug_red),
        Decor("rug_blue", DecorType.RUG, 80.0, R.string.d_rug_blue),
        Decor("rug_round", DecorType.RUG, 140.0, R.string.d_rug_round),
        Decor("rug_fur", DecorType.RUG, 160.0, R.string.d_rug_fur),
        Decor("rug_star", DecorType.RUG, 260.0, R.string.d_rug_star, 15),
        Decor("rug_royal", DecorType.RUG, 350.0, R.string.d_rug_royal, 25),

        Decor("wp_plain", DecorType.WALLPAPER, 0.0, R.string.d_wp_plain),
        Decor("wp_stripes", DecorType.WALLPAPER, 120.0, R.string.d_wp_stripes),
        Decor("wp_dots", DecorType.WALLPAPER, 120.0, R.string.d_wp_dots),
        Decor("wp_bricks", DecorType.WALLPAPER, 160.0, R.string.d_wp_bricks),
        Decor("wp_leaves", DecorType.WALLPAPER, 180.0, R.string.d_wp_leaves),
        Decor("wp_wood", DecorType.WALLPAPER, 200.0, R.string.d_wp_wood),
        Decor("wp_castle", DecorType.WALLPAPER, 220.0, R.string.d_wp_castle, 12),
        Decor("wp_stars", DecorType.WALLPAPER, 350.0, R.string.d_wp_stars, 15),

        Decor("fl_planks", DecorType.FLOOR, 0.0, R.string.d_fl_planks),
        Decor("fl_tiles", DecorType.FLOOR, 120.0, R.string.d_fl_tiles),
        Decor("fl_stone", DecorType.FLOOR, 160.0, R.string.d_fl_stone),
        Decor("fl_parquet", DecorType.FLOOR, 180.0, R.string.d_fl_parquet),
        Decor("fl_carpet", DecorType.FLOOR, 200.0, R.string.d_fl_carpet),
        Decor("fl_checker", DecorType.FLOOR, 260.0, R.string.d_fl_checker, 10),
        Decor("fl_marble", DecorType.FLOOR, 300.0, R.string.d_fl_marble, 18),

        Decor("apple_tree", DecorType.GARDEN_BIG, 200.0, R.string.d_apple_tree),
        Decor("well", DecorType.GARDEN_BIG, 260.0, R.string.d_well),
        Decor("swing", DecorType.GARDEN_BIG, 300.0, R.string.d_swing),
        Decor("pond", DecorType.GARDEN_BIG, 350.0, R.string.d_pond, 8),
        Decor("garden_shed", DecorType.GARDEN_BIG, 420.0, R.string.d_garden_shed, 10),
        Decor("fountain", DecorType.GARDEN_BIG, 550.0, R.string.d_fountain, 15),
        Decor("windmill", DecorType.GARDEN_BIG, 900.0, R.string.d_windmill, 25),

        Decor("flower_bed", DecorType.GARDEN_SMALL, 60.0, R.string.d_flower_bed),
        Decor("mailbox", DecorType.GARDEN_SMALL, 70.0, R.string.d_mailbox),
        Decor("gnome", DecorType.GARDEN_SMALL, 90.0, R.string.d_gnome),
        Decor("pumpkin_patch", DecorType.GARDEN_SMALL, 100.0, R.string.d_pumpkin_patch),
        Decor("birdhouse", DecorType.GARDEN_SMALL, 110.0, R.string.d_birdhouse, 4),
        Decor("lamp_post", DecorType.GARDEN_SMALL, 120.0, R.string.d_lamp_post),
        Decor("bench", DecorType.GARDEN_SMALL, 140.0, R.string.d_bench),
        Decor("beehive", DecorType.GARDEN_SMALL, 150.0, R.string.d_beehive, 6),
        Decor("garden_scarecrow", DecorType.GARDEN_SMALL, 160.0, R.string.d_garden_scarecrow, 8),

        Decor("fence_picket", DecorType.FENCE, 80.0, R.string.d_fence_picket),
        Decor("fence_hedge", DecorType.FENCE, 150.0, R.string.d_fence_hedge),
        Decor("fence_stone", DecorType.FENCE, 220.0, R.string.d_fence_stone, 10),
        Decor("fence_iron", DecorType.FENCE, 350.0, R.string.d_fence_iron, 20),

        Decor("path_dirt", DecorType.PATH, 40.0, R.string.d_path_dirt),
        Decor("path_stone", DecorType.PATH, 120.0, R.string.d_path_stone),
        Decor("path_brick", DecorType.PATH, 200.0, R.string.d_path_brick, 8),
    )

    fun item(key: String) = items.firstOrNull { it.key == key }

    /** "WALL_L=clock,RUG=rug_red" ⇄ map. */
    fun placement(csv: String): Map<Spot, String> = csv.split(',').mapNotNull { e ->
        val (s, k) = e.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        (runCatching { Spot.valueOf(s) }.getOrNull() ?: return@mapNotNull null) to k
    }.toMap()

    fun encode(m: Map<Spot, String>) = m.entries.joinToString(",") { "${it.key.name}=${it.value}" }
}
