package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R
import app.dayquest.pixel.Color
import java.time.LocalDate
import java.time.temporal.IsoFields
import kotlin.random.Random

enum class Slot { WEAPON, ARMOR, HEAD, SHIELD }

/** [noun] names generated (forged) items: "Sword", "Меч|m". */
enum class GearShape(@StringRes val noun: Int) {
    SWORD(R.string.n_sword), STAFF(R.string.n_staff), AXE(R.string.n_axe),
    TUNIC(R.string.n_tunic), PLATE(R.string.n_plate), ROBE(R.string.n_robe),
    CAP(R.string.n_cap), HELM(R.string.n_helm), WIZARD(R.string.n_hat), HORNED(R.string.n_horns), CROWN(R.string.n_crown),
    FLOWER(R.string.n_wreath), SUNHAT(R.string.n_hat), PUMPKIN(R.string.n_hat), BEANIE(R.string.n_hat),
    ROUND(R.string.n_buckler), KITE(R.string.n_shield), BOOK(R.string.n_tome),
    DAGGER(R.string.n_dagger), SPEAR(R.string.n_spear), HAMMER(R.string.n_hammer), BOW(R.string.n_bow), SCYTHE(R.string.n_scythe), WAND(R.string.n_wand),
    CLOAK(R.string.n_cloak), HOOD(R.string.n_hood), BANDANA(R.string.n_bandana), CIRCLET(R.string.n_circlet), FEATHER(R.string.n_cap),
    TOWER(R.string.n_shield), LANTERN(R.string.n_lantern), ORB(R.string.n_orb),
}

enum class Rarity(val color: Int, @StringRes val label: Int) {
    COMMON(0xFF9AA3B5.toInt(), R.string.rarity_common),
    RARE(0xFF4F9BEF.toInt(), R.string.rarity_rare),
    EPIC(0xFFA66BFF.toInt(), R.string.rarity_epic),
    LEGENDARY(0xFFF2A93B.toInt(), R.string.rarity_legendary),
}

data class Gear(
    val key: String,
    val slot: Slot,
    val shape: GearShape,
    val level: Int,
    val price: Double,
    val stats: Stats,
    val primary: Int,
    val secondary: Int,
    /** String resource of a hand-named item; 0 for forged ones (see [prefix] and [tier]). */
    @StringRes val name: Int,
    /** Enchanted items shimmer in the scene. */
    val glow: Boolean = false,
    val rarity: Rarity = Rarity.COMMON,
    /** Only sold during this season (kept forever once bought). */
    val season: Season? = null,
    /** Forged items: tier number and an index into the prefix list. */
    val tier: Int = 0,
    val prefix: Int = 0,
)

/** Seasons follow the real calendar, so the collection keeps growing for years. */
enum class Season(val key: String, val months: Set<Int>, @StringRes val label: Int) {
    SPRING("spring", setOf(3, 4, 5), R.string.season_spring),
    SUMMER("summer", setOf(6, 7, 8), R.string.season_summer),
    AUTUMN("autumn", setOf(9, 10, 11), R.string.season_autumn),
    WINTER("winter", setOf(12, 1, 2), R.string.season_winter);

    companion object {
        private fun shifted(date: LocalDate, southern: Boolean) = if (southern) date.plusMonths(6) else date

        fun of(date: LocalDate, southern: Boolean): Season = shifted(date, southern).monthValue.let { m -> entries.first { m in it.months } }

        /** Which year's season this is: a winter spanning December–February counts as one. */
        fun year(date: LocalDate, southern: Boolean): Int = shifted(date, southern).minusMonths(2).year

        fun byKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

enum class Species(val key: String, val color: Int, val belly: Int, val favorite: Food, val questOnly: Boolean, @StringRes val label: Int) {
    FOX("fox", 0xFFE8803A.toInt(), 0xFFF7E3CF.toInt(), Food.BERRY, false, R.string.sp_fox),
    CAT("cat", 0xFF9A93A8.toInt(), 0xFFF1E9E4.toInt(), Food.FISH, false, R.string.sp_cat),
    OWL("owl", 0xFFA0714A.toInt(), 0xFFF0D9B5.toInt(), Food.MEAT, false, R.string.sp_owl),
    SLIME("slime", 0xFF5CCB6F.toInt(), 0xFFC8F5C0.toInt(), Food.CAKE, false, R.string.sp_slime),
    BUNNY("bunny", 0xFFEDE1D4.toInt(), 0xFFF6B8C8.toInt(), Food.BERRY, false, R.string.sp_bunny),
    WOLF("wolf", 0xFF7C8796.toInt(), 0xFFE3E7EC.toInt(), Food.MEAT, false, R.string.sp_wolf),
    TURTLE("turtle", 0xFF5FA35A.toInt(), 0xFFB8834A.toInt(), Food.FISH, true, R.string.sp_turtle),
    DRAGON("dragon", 0xFFD9534F.toInt(), 0xFFF2C14E.toInt(), Food.MEAT, true, R.string.sp_dragon);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

/** Hatching potions. [color] null keeps the species' own colours; seasonal ones paint the belly too. */
enum class Potion(
    val key: String,
    val color: Int?,
    val swatch: Int,
    val questOnly: Boolean,
    @StringRes val label: Int,
    val season: Season? = null,
    val belly: Int? = null,
) {
    BASE("base", null, 0xFFC9A27A.toInt(), false, R.string.po_base),
    GOLDEN("golden", 0xFFF2C14E.toInt(), 0xFFF2C14E.toInt(), false, R.string.po_golden),
    MOSS("moss", 0xFF6DBA5A.toInt(), 0xFF6DBA5A.toInt(), false, R.string.po_moss),
    ROSE("rose", 0xFFF28DB2.toInt(), 0xFFF28DB2.toInt(), false, R.string.po_rose),
    FROST("frost", 0xFF9FE3FF.toInt(), 0xFF9FE3FF.toInt(), true, R.string.po_frost),
    SHADOW("shadow", 0xFF6A5AA6.toInt(), 0xFF6A5AA6.toInt(), true, R.string.po_shadow),
    EMBER("ember", 0xFFFF6A3D.toInt(), 0xFFFF6A3D.toInt(), true, R.string.po_ember),
    BLOSSOM("blossom", 0xFFF7C4DA.toInt(), 0xFFF7C4DA.toInt(), false, R.string.po_blossom, Season.SPRING, 0xFF9BDB8A.toInt()),
    TIDE("tide", 0xFF46B8D8.toInt(), 0xFF46B8D8.toInt(), false, R.string.po_tide, Season.SUMMER, 0xFFFFE3A3.toInt()),
    HARVEST("harvest", 0xFFE8862E.toInt(), 0xFFE8862E.toInt(), false, R.string.po_harvest, Season.AUTUMN, 0xFF8A3B2E.toInt()),
    AURORA("aurora", 0xFF6FE3C8.toInt(), 0xFF6FE3C8.toInt(), false, R.string.po_aurora, Season.WINTER, 0xFF9B82FF.toInt());

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
        fun of(season: Season) = entries.first { it.season == season }
    }
}

enum class Food(val key: String, @StringRes val label: Int) {
    BERRY("berry", R.string.food_berry),
    FISH("fish", R.string.food_fish),
    MEAT("meat", R.string.food_meat),
    CAKE("cake", R.string.food_cake);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

enum class BossShape(@StringRes val noun: Int) {
    SLIME(R.string.bn_slime), GOBLIN(R.string.bn_goblin), GOLEM(R.string.bn_golem), WYRM(R.string.bn_wyrm),
    LICH(R.string.bn_lich), DRAGON(R.string.bn_dragon), YETI(R.string.bn_yeti), TREANT(R.string.bn_treant),
    CRAB(R.string.bn_crab), PUMPKIN(R.string.bn_pumpkin),
    // Added later: new shapes go at the end so older weeks keep their foes.
    SPIDER(R.string.bn_spider), KRAKEN(R.string.bn_kraken), MIMIC(R.string.bn_mimic), GHOST(R.string.bn_ghost),
    KNIGHT(R.string.bn_knight), HYDRA(R.string.bn_hydra), BEE(R.string.bn_bee), MUSHROOM(R.string.bn_mushroom),
    JELLYFISH(R.string.bn_jellyfish), SCORPION(R.string.bn_scorpion), SCARECROW(R.string.bn_scarecrow), BAT(R.string.bn_bat),
    SNOWMAN(R.string.bn_snowman), PENGUIN(R.string.bn_penguin),
}

enum class BossKind { MAIN, SEASONAL, WEEKLY }

data class Boss(
    /** The fight's full key, e.g. "slime_king", "slime_king#2", "season:winter:2026:L12", "week:2026:42:L12". */
    val key: String,
    val level: Int,
    val hp: Double,
    /** Damage per missed daily, scaled by its difficulty. */
    val str: Double,
    val gold: Double,
    val xp: Double,
    val rewards: List<Pair<String, Int>>,
    val shape: BossShape,
    val primary: Int,
    val secondary: Int,
    @StringRes val name: Int,
    @StringRes val story: Int,
    /** 0 normal, 1 elite, 2 ancient, 3+ mythic. */
    val rank: Int = 0,
    val kind: BossKind = BossKind.MAIN,
    /** Weekly bosses: index into the adjective list. */
    val adjective: Int = 0,
    val season: Season? = null,
) {
    /** What a win is recorded under: rank and level stripped. */
    val record: String get() = Catalog.recordKey(key)
}

enum class SceneKind { MEADOW, FOREST, SHORE, SNOW, DESERT, CAVE, GROVE, CASTLE, VOLCANO, SKY }

data class SceneDef(val key: String, val kind: SceneKind, val price: Double, @StringRes val name: Int)

object Catalog {
    val fixedGear = listOf(
        Gear("wooden_sword", Slot.WEAPON, GearShape.SWORD, 1, 0.0, Stats(str = 1), 0xFFB07A45.toInt(), 0xFF6B4A2B.toInt(), R.string.g_wooden_sword),
        Gear("iron_sword", Slot.WEAPON, GearShape.SWORD, 2, 20.0, Stats(str = 3), 0xFFC4CED9.toInt(), 0xFF7A4E2D.toInt(), R.string.g_iron_sword),
        Gear("apprentice_staff", Slot.WEAPON, GearShape.STAFF, 3, 30.0, Stats(int = 4), 0xFF5EC8FF.toInt(), 0xFF8A5A36.toInt(), R.string.g_apprentice_staff, glow = true),
        Gear("battle_axe", Slot.WEAPON, GearShape.AXE, 5, 60.0, Stats(str = 6), 0xFFB9C3CF.toInt(), 0xFF6B4A2B.toInt(), R.string.g_battle_axe),
        Gear("flame_blade", Slot.WEAPON, GearShape.SWORD, 7, 90.0, Stats(str = 8, per = 3), 0xFFFF7A3D.toInt(), 0xFF3B2A4A.toInt(), R.string.g_flame_blade, glow = true, rarity = Rarity.RARE),
        Gear("frost_staff", Slot.WEAPON, GearShape.STAFF, 10, 140.0, Stats(int = 10, con = 3), 0xFFB6F0FF.toInt(), 0xFFDDE6F5.toInt(), R.string.g_frost_staff, glow = true, rarity = Rarity.RARE),
        Gear("star_blade", Slot.WEAPON, GearShape.SWORD, 15, 240.0, Stats(str = 14, int = 6), 0xFFFFE17A.toInt(), 0xFF6B4CFF.toInt(), R.string.g_star_blade, glow = true, rarity = Rarity.EPIC),

        Gear("tunic", Slot.ARMOR, GearShape.TUNIC, 1, 0.0, Stats(), 0xFF4F7BD9.toInt(), 0xFF8A5A36.toInt(), R.string.g_tunic),
        Gear("leather", Slot.ARMOR, GearShape.TUNIC, 2, 25.0, Stats(con = 3), 0xFF9A6334.toInt(), 0xFF4A2F1E.toInt(), R.string.g_leather),
        Gear("chainmail", Slot.ARMOR, GearShape.PLATE, 5, 60.0, Stats(con = 6), 0xFF9AA6B4.toInt(), 0xFF5E6A7A.toInt(), R.string.g_chainmail),
        Gear("mystic_robe", Slot.ARMOR, GearShape.ROBE, 8, 110.0, Stats(int = 7, con = 3), 0xFF6A3FC8.toInt(), 0xFFF2C14E.toInt(), R.string.g_mystic_robe, rarity = Rarity.RARE),
        Gear("knight_plate", Slot.ARMOR, GearShape.PLATE, 11, 160.0, Stats(con = 11), 0xFFD6DEE8.toInt(), 0xFFE0B341.toInt(), R.string.g_knight_plate, rarity = Rarity.RARE),
        Gear("dragon_scale", Slot.ARMOR, GearShape.PLATE, 16, 260.0, Stats(con = 14, str = 5), 0xFF2FA36B.toInt(), 0xFFF2C14E.toInt(), R.string.g_dragon_scale, rarity = Rarity.EPIC),

        Gear("cap", Slot.HEAD, GearShape.CAP, 1, 10.0, Stats(per = 2), 0xFFD9534F.toInt(), 0xFFF2E8D5.toInt(), R.string.g_cap),
        Gear("iron_helm", Slot.HEAD, GearShape.HELM, 4, 35.0, Stats(con = 4), 0xFFA9B4C0.toInt(), 0xFF6E7A8A.toInt(), R.string.g_iron_helm),
        Gear("wizard_hat", Slot.HEAD, GearShape.WIZARD, 6, 60.0, Stats(int = 6), 0xFF3E3A9E.toInt(), 0xFFF2C14E.toInt(), R.string.g_wizard_hat),
        Gear("horned_helm", Slot.HEAD, GearShape.HORNED, 10, 120.0, Stats(str = 6, con = 4), 0xFF8E99A6.toInt(), 0xFFF3EAD3.toInt(), R.string.g_horned_helm, rarity = Rarity.RARE),
        Gear("crown", Slot.HEAD, GearShape.CROWN, 18, 320.0, Stats(per = 12, int = 6), 0xFFF2C14E.toInt(), 0xFFE2435F.toInt(), R.string.g_crown, glow = true, rarity = Rarity.EPIC),

        Gear("buckler", Slot.SHIELD, GearShape.ROUND, 3, 25.0, Stats(con = 3), 0xFF9A6334.toInt(), 0xFFC9D2DC.toInt(), R.string.g_buckler),
        Gear("kite_shield", Slot.SHIELD, GearShape.KITE, 6, 65.0, Stats(con = 6), 0xFF3D6FD9.toInt(), 0xFFE8C15A.toInt(), R.string.g_kite_shield),
        Gear("spell_tome", Slot.SHIELD, GearShape.BOOK, 8, 85.0, Stats(int = 7), 0xFF8E2F4F.toInt(), 0xFFF2C14E.toInt(), R.string.g_spell_tome, rarity = Rarity.RARE),
        Gear("aegis", Slot.SHIELD, GearShape.KITE, 14, 220.0, Stats(con = 12, per = 4), 0xFFE8EEF5.toInt(), 0xFF3FB7C9.toInt(), R.string.g_aegis, glow = true, rarity = Rarity.EPIC),

        Gear("dagger", Slot.WEAPON, GearShape.DAGGER, 2, 18.0, Stats(per = 3), 0xFFC4CED9.toInt(), 0xFF4A3424.toInt(), R.string.g_dagger),
        Gear("spear", Slot.WEAPON, GearShape.SPEAR, 4, 45.0, Stats(str = 4, per = 2), 0xFFC4CED9.toInt(), 0xFF8A5A36.toInt(), R.string.g_spear),
        Gear("willow_wand", Slot.WEAPON, GearShape.WAND, 6, 70.0, Stats(int = 6), 0xFFFFE17A.toInt(), 0xFF8A5A36.toInt(), R.string.g_willow_wand, glow = true),
        Gear("war_hammer", Slot.WEAPON, GearShape.HAMMER, 9, 120.0, Stats(str = 9, con = 2), 0xFF9AA6B4.toInt(), 0xFF5A3A24.toInt(), R.string.g_war_hammer, rarity = Rarity.RARE),
        Gear("elven_bow", Slot.WEAPON, GearShape.BOW, 12, 180.0, Stats(per = 10, str = 3), 0xFFE0B341.toInt(), 0xFFF4F8FF.toInt(), R.string.g_elven_bow, rarity = Rarity.RARE),
        Gear("moon_scythe", Slot.WEAPON, GearShape.SCYTHE, 17, 290.0, Stats(str = 12, int = 8), 0xFFB6C8FF.toInt(), 0xFF3A3550.toInt(), R.string.g_moon_scythe, glow = true, rarity = Rarity.EPIC),
        Gear("sun_hammer", Slot.WEAPON, GearShape.HAMMER, 19, 330.0, Stats(str = 16, con = 6), 0xFFFFC94A.toInt(), 0xFF8A3B2E.toInt(), R.string.g_sun_hammer, glow = true, rarity = Rarity.EPIC),

        Gear("ranger_cloak", Slot.ARMOR, GearShape.CLOAK, 4, 50.0, Stats(per = 4, con = 1), 0xFF6B7F4A.toInt(), 0xFF3E5A3A.toInt(), R.string.g_ranger_cloak),
        Gear("sunsteel", Slot.ARMOR, GearShape.PLATE, 13, 200.0, Stats(con = 12, str = 3), 0xFFF2D27A.toInt(), 0xFFE2435F.toInt(), R.string.g_sunsteel, rarity = Rarity.RARE),
        Gear("shadow_cloak", Slot.ARMOR, GearShape.CLOAK, 15, 250.0, Stats(per = 10, str = 4), 0xFF3A3550.toInt(), 0xFF6A3FC8.toInt(), R.string.g_shadow_cloak, rarity = Rarity.EPIC),
        Gear("archmage_robe", Slot.ARMOR, GearShape.ROBE, 19, 330.0, Stats(int = 14, con = 5), 0xFF1E2A5E.toInt(), 0xFF7FE3F0.toInt(), R.string.g_archmage_robe, rarity = Rarity.EPIC),

        Gear("hood", Slot.HEAD, GearShape.HOOD, 2, 20.0, Stats(per = 2, con = 1), 0xFF7A8A5A.toInt(), 0xFF5A6A3A.toInt(), R.string.g_hood),
        Gear("bandana", Slot.HEAD, GearShape.BANDANA, 3, 25.0, Stats(str = 2, per = 1), 0xFFE2435F.toInt(), 0xFFF6EBDD.toInt(), R.string.g_bandana),
        Gear("feather_cap", Slot.HEAD, GearShape.FEATHER, 7, 80.0, Stats(per = 6), 0xFF3E8A5A.toInt(), 0xFFE2435F.toInt(), R.string.g_feather_cap),
        Gear("circlet", Slot.HEAD, GearShape.CIRCLET, 12, 170.0, Stats(int = 8, per = 4), 0xFFD6DEE8.toInt(), 0xFF5EC8FF.toInt(), R.string.g_circlet, glow = true, rarity = Rarity.RARE),

        Gear("tower_shield", Slot.SHIELD, GearShape.TOWER, 9, 110.0, Stats(con = 9), 0xFF8E99A6.toInt(), 0xFFE2435F.toInt(), R.string.g_tower_shield),
        Gear("wisp_lantern", Slot.SHIELD, GearShape.LANTERN, 11, 150.0, Stats(int = 5, per = 5), 0xFF5A4636.toInt(), 0xFF7CFFB2.toInt(), R.string.g_wisp_lantern, glow = true, rarity = Rarity.RARE),
        Gear("crystal_orb", Slot.SHIELD, GearShape.ORB, 16, 260.0, Stats(int = 12, per = 4), 0xFFF2C14E.toInt(), 0xFFB79CFF.toInt(), R.string.g_crystal_orb, glow = true, rarity = Rarity.EPIC),

        // Seasonal hats: sold only in their season, a reason to come back every quarter.
        Gear("flower_crown", Slot.HEAD, GearShape.FLOWER, 1, 90.0, Stats(per = 4, int = 3), 0xFFF28DB2.toInt(), 0xFF6DBA5A.toInt(), R.string.g_flower_crown, season = Season.SPRING, rarity = Rarity.RARE),
        Gear("sun_hat", Slot.HEAD, GearShape.SUNHAT, 1, 90.0, Stats(con = 4, per = 3), 0xFFF2D27A.toInt(), 0xFFE2435F.toInt(), R.string.g_sun_hat, season = Season.SUMMER, rarity = Rarity.RARE),
        Gear("pumpkin_hat", Slot.HEAD, GearShape.PUMPKIN, 1, 90.0, Stats(str = 4, con = 3), 0xFFE8862E.toInt(), 0xFF4E9F5A.toInt(), R.string.g_pumpkin_hat, season = Season.AUTUMN, rarity = Rarity.RARE),
        Gear("winter_hat", Slot.HEAD, GearShape.BEANIE, 1, 90.0, Stats(int = 4, con = 3), 0xFF4F7BD9.toInt(), 0xFFF4F8FF.toInt(), R.string.g_winter_hat, season = Season.WINTER, rarity = Rarity.RARE),
        // ...and a seasonal weapon and outfit to go with each hat.
        Gear("blossom_wand", Slot.WEAPON, GearShape.WAND, 1, 120.0, Stats(int = 5, per = 3), 0xFFF28DB2.toInt(), 0xFF8A5A36.toInt(), R.string.g_blossom_wand, glow = true, season = Season.SPRING, rarity = Rarity.RARE),
        Gear("petal_cloak", Slot.ARMOR, GearShape.CLOAK, 1, 120.0, Stats(con = 4, per = 3), 0xFFF7E3EC.toInt(), 0xFF6DBA5A.toInt(), R.string.g_petal_cloak, season = Season.SPRING, rarity = Rarity.RARE),
        Gear("coral_trident", Slot.WEAPON, GearShape.SPEAR, 1, 120.0, Stats(str = 5, per = 3), 0xFFFF8A7A.toInt(), 0xFF46B8D8.toInt(), R.string.g_coral_trident, season = Season.SUMMER, rarity = Rarity.RARE),
        Gear("island_shirt", Slot.ARMOR, GearShape.TUNIC, 1, 120.0, Stats(con = 3, per = 4), 0xFF46B8D8.toInt(), 0xFFFFE3A3.toInt(), R.string.g_island_shirt, season = Season.SUMMER, rarity = Rarity.RARE),
        Gear("harvest_scythe", Slot.WEAPON, GearShape.SCYTHE, 1, 120.0, Stats(str = 6, con = 2), 0xFFD6DEE8.toInt(), 0xFF8A5A36.toInt(), R.string.g_harvest_scythe, season = Season.AUTUMN, rarity = Rarity.RARE),
        Gear("leaf_cloak", Slot.ARMOR, GearShape.CLOAK, 1, 120.0, Stats(con = 4, per = 3), 0xFF8A3B2E.toInt(), 0xFFE8862E.toInt(), R.string.g_leaf_cloak, season = Season.AUTUMN, rarity = Rarity.RARE),
        Gear("snowflake_staff", Slot.WEAPON, GearShape.STAFF, 1, 120.0, Stats(int = 6, con = 2), 0xFFDDF2FF.toInt(), 0xFF4F7BD9.toInt(), R.string.g_snowflake_staff, glow = true, season = Season.WINTER, rarity = Rarity.RARE),
        Gear("fur_cloak", Slot.ARMOR, GearShape.CLOAK, 1, 120.0, Stats(con = 5, int = 2), 0xFFC8553D.toInt(), 0xFFF4F8FF.toInt(), R.string.g_fur_cloak, season = Season.WINTER, rarity = Rarity.RARE),
    )

    /** Fixed catalog plus forged tiers up to the given one. */
    fun gearUpTo(tier: Int) = fixedGear + (1..tier).flatMap { t -> Slot.entries.map { Forge.item(t, it) } }

    fun gear(key: String): Gear? = if (key.startsWith("gen:")) Forge.parse(key) else fixedGear.firstOrNull { it.key == key }

    // ---- Bosses --------------------------------------------------------------------------

    val bosses = listOf(
        Boss("slime_king", 1, 100.0, 0.6, 30.0, 60.0, listOf("potion:golden" to 3), BossShape.SLIME, 0xFF5CCB6F.toInt(), 0xFFF2C14E.toInt(), R.string.b_slime_king, R.string.bs_slime_king),
        Boss("goblin_chief", 3, 200.0, 0.9, 50.0, 120.0, listOf("egg:wolf" to 2, "food:meat" to 3), BossShape.GOBLIN, 0xFF7DAA4A.toInt(), 0xFF8A5A36.toInt(), R.string.b_goblin_chief, R.string.bs_goblin_chief),
        Boss("stone_golem", 6, 350.0, 1.1, 80.0, 200.0, listOf("egg:turtle" to 2), BossShape.GOLEM, 0xFF8E8A99.toInt(), 0xFF5EC8FF.toInt(), R.string.b_stone_golem, R.string.bs_stone_golem),
        Boss("frost_wyrm", 9, 500.0, 1.4, 120.0, 300.0, listOf("potion:frost" to 3), BossShape.WYRM, 0xFF9FD8F5.toInt(), 0xFF3E5FBF.toInt(), R.string.b_frost_wyrm, R.string.bs_frost_wyrm),
        Boss("shadow_lich", 13, 700.0, 1.7, 170.0, 420.0, listOf("potion:shadow" to 3), BossShape.LICH, 0xFF4B3F72.toInt(), 0xFF7CFFB2.toInt(), R.string.b_shadow_lich, R.string.bs_shadow_lich),
        Boss("ember_dragon", 18, 1000.0, 2.0, 250.0, 600.0, listOf("egg:dragon" to 2, "potion:ember" to 3), BossShape.DRAGON, 0xFFD9443F.toInt(), 0xFFF2C14E.toInt(), R.string.b_ember_dragon, R.string.bs_ember_dragon),
        // The road goes on past the dragon.
        Boss("scroll_spider", 22, 1300.0, 2.2, 320.0, 750.0, listOf("potion:shadow" to 3, "egg:dragon" to 1), BossShape.SPIDER, 0xFF6E5E96.toInt(), 0xFFE2435F.toInt(), R.string.b_spider, R.string.bs_spider),
        Boss("ping_kraken", 26, 1600.0, 2.4, 400.0, 900.0, listOf("egg:turtle" to 3, "potion:frost" to 2), BossShape.KRAKEN, 0xFF8E5AC8.toInt(), 0xFFFFE066.toInt(), R.string.b_kraken, R.string.bs_kraken),
        Boss("impulse_mimic", 30, 2000.0, 2.6, 520.0, 1050.0, listOf("potion:golden" to 5, "food:cake" to 5), BossShape.MIMIC, 0xFFA0643A.toInt(), 0xFFF2C14E.toInt(), R.string.b_mimic, R.string.bs_mimic),
        Boss("unfinished_ghost", 35, 2400.0, 2.8, 600.0, 1200.0, listOf("potion:shadow" to 4, "egg:wolf" to 2), BossShape.GHOST, 0xFFDDE8F5.toInt(), 0xFF7CFFB2.toInt(), R.string.b_ghost, R.string.bs_ghost),
        Boss("perfect_knight", 40, 2900.0, 3.1, 700.0, 1400.0, listOf("potion:ember" to 4, "egg:dragon" to 2), BossShape.KNIGHT, 0xFF4A4A5E.toInt(), 0xFFE2435F.toInt(), R.string.b_knight, R.string.bs_knight),
        Boss("promise_hydra", 45, 3500.0, 3.4, 800.0, 1600.0, listOf("egg:dragon" to 3, "potion:ember" to 3, "potion:frost" to 3), BossShape.HYDRA, 0xFF3FA37A.toInt(), 0xFFF2D27A.toInt(), R.string.b_hydra, R.string.bs_hydra),
    )

    private data class SeasonalTemplate(val shape: BossShape, val primary: Int, val secondary: Int, @StringRes val name: Int, @StringRes val story: Int)

    /** Three foes per season, each fought once per season. The first keeps its old key for saved fights. */
    private val seasonal = mapOf(
        Season.SPRING to listOf(
            SeasonalTemplate(BossShape.TREANT, 0xFF8A5A36.toInt(), 0xFFF7C4DA.toInt(), R.string.b_treant, R.string.bs_treant),
            SeasonalTemplate(BossShape.BEE, 0xFFF2C14E.toInt(), 0xFFF28DB2.toInt(), R.string.b_bee, R.string.bs_bee),
            SeasonalTemplate(BossShape.MUSHROOM, 0xFF6FA35A.toInt(), 0xFFEFE2C8.toInt(), R.string.b_mushroom, R.string.bs_mushroom),
        ),
        Season.SUMMER to listOf(
            SeasonalTemplate(BossShape.CRAB, 0xFFE5574F.toInt(), 0xFFFFE3A3.toInt(), R.string.b_crab, R.string.bs_crab),
            SeasonalTemplate(BossShape.JELLYFISH, 0xFFB79CFF.toInt(), 0xFF7FE3F0.toInt(), R.string.b_jellyfish, R.string.bs_jellyfish),
            SeasonalTemplate(BossShape.SCORPION, 0xFFD9A040.toInt(), 0xFF7CFFB2.toInt(), R.string.b_scorpion, R.string.bs_scorpion),
        ),
        Season.AUTUMN to listOf(
            SeasonalTemplate(BossShape.PUMPKIN, 0xFFE8862E.toInt(), 0xFF4E9F5A.toInt(), R.string.b_pumpkin, R.string.bs_pumpkin),
            SeasonalTemplate(BossShape.SCARECROW, 0xFF8A5A9E.toInt(), 0xFF3A3550.toInt(), R.string.b_scarecrow, R.string.bs_scarecrow),
            SeasonalTemplate(BossShape.BAT, 0xFF5A4A7A.toInt(), 0xFFC8553D.toInt(), R.string.b_bat, R.string.bs_bat),
        ),
        Season.WINTER to listOf(
            SeasonalTemplate(BossShape.YETI, 0xFFEAF2FF.toInt(), 0xFF6FB6E8.toInt(), R.string.b_yeti, R.string.bs_yeti),
            SeasonalTemplate(BossShape.SNOWMAN, 0xFFF4F8FF.toInt(), 0xFFE2435F.toInt(), R.string.b_snowman, R.string.bs_snowman),
            SeasonalTemplate(BossShape.PENGUIN, 0xFF3A3F5E.toInt(), 0xFFF2C14E.toInt(), R.string.b_penguin, R.string.bs_penguin),
        ),
    )

    const val SEASONAL_PER_SEASON = 3

    /** Rank names for rematches. */
    const val ELITE = 1
    const val ANCIENT = 2
    const val MYTHIC = 3

    fun recordKey(key: String): String = when {
        key.startsWith("week:") || key.startsWith("season:") -> key.substringBefore(":L")
        else -> key.substringBefore('#')
    }

    fun boss(key: String): Boss? {
        if (key.isEmpty()) return null
        if (key.startsWith("week:")) {
            val p = key.split(':')
            return weekly(p.getOrNull(1)?.toIntOrNull() ?: return null, p.getOrNull(2)?.toIntOrNull() ?: return null, p.getOrNull(3)?.removePrefix("L")?.toIntOrNull() ?: 1)
        }
        if (key.startsWith("season:")) {
            val p = key.split(':')
            val id = (p.getOrNull(1) ?: return null).split('~')
            val s = Season.byKey(id[0]) ?: return null
            val variant = id.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0 until SEASONAL_PER_SEASON } ?: 0
            return seasonalBoss(s, p.getOrNull(2)?.toIntOrNull() ?: return null, p.getOrNull(3)?.removePrefix("L")?.toIntOrNull() ?: 1, variant)
        }
        val base = bosses.firstOrNull { it.key == key.substringBefore('#') } ?: return null
        val rank = key.substringAfter('#', "0").toIntOrNull() ?: 0
        return if (rank == 0) base else ranked(base, rank)
    }

    /** Rematches: the same foe, recoloured and tougher, with richer loot. */
    fun ranked(b: Boss, rank: Int): Boss {
        val r = rank.coerceAtLeast(1)
        fun shift(c: Int): Int {
            val hsv = Color.toHsv(c)
            return Color.fromHsv(hsv[0] + 47f * r, (hsv[1] + 0.08f * r).coerceAtMost(1f), (hsv[2] - 0.04f * r).coerceAtLeast(0.35f))
        }
        val rare = listOf("potion:frost", "potion:shadow", "potion:ember", "egg:turtle", "egg:dragon")
        val bonus = rare[(bosses.indexOf(b) + r) % rare.size]
        return b.copy(
            key = "${b.key}#$r",
            hp = b.hp * (1 + 0.8 * r),
            str = b.str * (1 + 0.3 * r),
            gold = b.gold * (1 + 0.6 * r),
            xp = b.xp * (1 + 0.6 * r),
            // Same item twice adds up instead of showing as two lines.
            rewards = (b.rewards + (bonus to 1 + r / 2)).groupBy({ it.first }, { it.second }).map { (k, n) -> k to n.sum() },
            primary = shift(b.primary),
            secondary = shift(b.secondary),
            rank = r,
        )
    }

    /** Next fight against a main boss: plain the first time, then elite, ancient, mythic. */
    fun nextFight(b: Boss, wins: Int): Boss = if (wins == 0) b else ranked(b, wins)

    fun seasonalBosses(s: Season, year: Int, level: Int) = (0 until SEASONAL_PER_SEASON).map { seasonalBoss(s, year, level, it) }

    fun seasonalBoss(s: Season, year: Int, level: Int, variant: Int = 0): Boss {
        val t = seasonal.getValue(s)[variant]
        val potion = Potion.of(s).key
        val id = if (variant == 0) s.key else "${s.key}~$variant"
        // Sized to take about a month of steady days; the second foe is quicker, the third tougher.
        val size = when (variant) { 1 -> 0.8; 2 -> 1.2; else -> 1.0 }
        val egg = basicEggs()[(year + s.ordinal) % basicEggs().size].key
        return Boss(
            key = "season:$id:$year:L$level", level = 1,
            hp = (500.0 + level * 18) * size, str = 1.0 + level * 0.04, gold = (100.0 + level * 5) * size, xp = (150.0 + level * 10) * size,
            rewards = when (variant) {
                1 -> listOf("potion:$potion" to 2, "food:berry" to 4, "egg:$egg" to 1)
                2 -> listOf("potion:$potion" to 3, "potion:golden" to 2, "food:cake" to 3)
                else -> listOf("potion:$potion" to 3, "food:cake" to 2)
            },
            shape = t.shape, primary = t.primary, secondary = t.secondary, name = t.name, story = t.story,
            kind = BossKind.SEASONAL, season = s,
        )
    }

    private val weeklyColors = listOf(
        0xFF5CCB6F to 0xFFF2C14E, 0xFFB45CFF to 0xFF7CFFB2, 0xFF3FB7C9 to 0xFFFFE17A, 0xFFE5574F to 0xFF2E2433,
        0xFFF28DB2 to 0xFF5EC8FF, 0xFF8E99A6 to 0xFFFF7A3D, 0xFF4F7BD9 to 0xFFF4F8FF, 0xFFE8C15A to 0xFF6A3FC8,
    )

    const val WEEKLY_ADJECTIVES = 8

    /** A new wandering foe every week, built from a seed: shape, colours and name. */
    fun weekly(year: Int, week: Int, level: Int): Boss {
        val r = Random(year * 100 + week)
        // Weeks before the bestiary grew keep the foe they always had.
        val pool = if (year * 100 + week <= 202642) BossShape.entries.take(10) else BossShape.entries
        val shape = pool[r.nextInt(pool.size)]
        val (p, s) = weeklyColors[r.nextInt(weeklyColors.size)]
        val rare = listOf("potion:frost", "potion:shadow", "potion:ember", "egg:turtle", "egg:dragon", "potion:golden", "potion:rose")
        return Boss(
            key = "week:$year:$week:L$level", level = 1,
            // Sized to fall within a week of ordinary days (about 8 finished tasks a day).
            hp = 150.0 + level * 3.5, str = 0.8 + level * 0.03, gold = 30.0 + level * 2, xp = 60.0 + level * 6,
            rewards = listOf(rare[r.nextInt(rare.size)] to 2, "food:" + Food.entries[r.nextInt(Food.entries.size)].key to 3),
            shape = shape, primary = p.toInt(), secondary = s.toInt(), name = shape.noun, story = R.string.bs_weekly,
            kind = BossKind.WEEKLY, adjective = r.nextInt(WEEKLY_ADJECTIVES),
        )
    }

    fun weekOf(date: LocalDate) = date.get(IsoFields.WEEK_BASED_YEAR) to date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    // ---- Scenes & looks ------------------------------------------------------------------

    val scenes = listOf(
        SceneDef("meadow", SceneKind.MEADOW, 0.0, R.string.sc_meadow),
        SceneDef("forest", SceneKind.FOREST, 40.0, R.string.sc_forest),
        SceneDef("shore", SceneKind.SHORE, 60.0, R.string.sc_shore),
        SceneDef("snow", SceneKind.SNOW, 80.0, R.string.sc_snow),
        SceneDef("desert", SceneKind.DESERT, 80.0, R.string.sc_desert),
        SceneDef("cave", SceneKind.CAVE, 150.0, R.string.sc_cave),
        SceneDef("grove", SceneKind.GROVE, 180.0, R.string.sc_grove),
        SceneDef("castle", SceneKind.CASTLE, 240.0, R.string.sc_castle),
        SceneDef("volcano", SceneKind.VOLCANO, 320.0, R.string.sc_volcano),
        SceneDef("sky", SceneKind.SKY, 450.0, R.string.sc_sky),
    )

    fun scene(key: String) = scenes.firstOrNull { it.key == key } ?: scenes.first()

    val skins = intArrayOf(0xFFFBE1CF.toInt(), 0xFFF3C9A8.toInt(), 0xFFE0A87E.toInt(), 0xFFC18358.toInt(), 0xFF96603C.toInt(), 0xFF6B4129.toInt())
    val hairColors = intArrayOf(
        0xFF2E2433.toInt(), 0xFF6B4129.toInt(), 0xFFB5682F.toInt(), 0xFFE8C26A.toInt(),
        0xFFE36B4A.toInt(), 0xFFEDEAF2.toInt(), 0xFF7A63E0.toInt(), 0xFF3FB7A0.toInt(), 0xFFF28DB2.toInt(),
    )
    const val HAIR_STYLES = 6

    /** Basic eggs and potions drop from tasks; quest ones come from bosses, seasonal ones only in their season. */
    fun basicEggs() = Species.entries.filter { !it.questOnly }
    fun basicPotions() = Potion.entries.filter { !it.questOnly && it.season == null }

    fun petKey(s: Species, p: Potion) = "${s.key}:${p.key}"
    fun petParts(key: String): Pair<Species, Potion>? {
        val (s, p) = key.split(':').takeIf { it.size == 2 } ?: return null
        return (Species.of(s) ?: return null) to (Potion.of(p) ?: return null)
    }

    val totalPets get() = Species.entries.size * Potion.entries.size
}

/**
 * The endless forge: from level 20, every 5 levels a new tier of gear appears, one item per slot.
 * Everything about an item (shape, colours, stats, name) comes from its tier and slot, so nothing is stored.
 */
object Forge {
    const val FIRST_LEVEL = 20
    const val STEP = 5
    const val PREFIXES = 12

    fun tierLevel(t: Int) = FIRST_LEVEL + STEP * (t - 1)

    /** Highest tier the hero can use at this level (0 below level 20). */
    fun tierFor(level: Int) = if (level < FIRST_LEVEL) 0 else (level - FIRST_LEVEL) / STEP + 1

    private val shapes = mapOf(
        Slot.WEAPON to listOf(GearShape.SWORD, GearShape.STAFF, GearShape.AXE),
        Slot.ARMOR to listOf(GearShape.PLATE, GearShape.ROBE, GearShape.TUNIC),
        Slot.HEAD to listOf(GearShape.HELM, GearShape.WIZARD, GearShape.HORNED, GearShape.CROWN, GearShape.CAP),
        Slot.SHIELD to listOf(GearShape.KITE, GearShape.BOOK, GearShape.ROUND),
    )

    private val palettes = listOf(
        0xFF6FE3C8 to 0xFF3E3A9E, 0xFFFF7A3D to 0xFF2E2433, 0xFFB6F0FF to 0xFFF2C14E, 0xFF9B82FF to 0xFFFFE17A,
        0xFF4FC07A to 0xFFF4F8FF, 0xFFE2435F to 0xFFF2C14E, 0xFFF2C14E to 0xFF6A3FC8, 0xFF3FB7C9 to 0xFFE8EEF5,
        0xFF2E2433 to 0xFFE070FF, 0xFFE8EEF5 to 0xFF5EC8FF, 0xFFE8862E to 0xFF4E9F5A, 0xFFF28DB2 to 0xFF7CFFB2,
    )

    fun key(t: Int, slot: Slot) = "gen:$t:${slot.name}"

    fun parse(key: String): Gear? {
        val p = key.split(':')
        val t = p.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        val slot = runCatching { Slot.valueOf(p.getOrNull(2) ?: "") }.getOrNull() ?: return null
        return item(t, slot)
    }

    fun item(t: Int, slot: Slot): Gear {
        val r = Random(t * 7919 + slot.ordinal * 104729)
        val rarity = when {
            t <= 2 -> Rarity.RARE
            t <= 5 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
        val shape = shapes.getValue(slot).let { it[(t + slot.ordinal) % it.size] }
        val (p, s) = palettes[(t * 5 + slot.ordinal * 3) % palettes.size]
        val budget = when (slot) {
            Slot.WEAPON -> 20 + 4 * t
            Slot.ARMOR -> 19 + 4 * t
            Slot.HEAD -> 18 + 3 * t
            Slot.SHIELD -> 16 + 3 * t
        }
        val main = budget * 6 / 10
        val stats = listOf(0, 1, 2, 3).shuffled(r).take(2).let { (a, b) ->
            fun one(i: Int, v: Int) = when (i) {
                0 -> Stats(str = v)
                1 -> Stats(int = v)
                2 -> Stats(con = v)
                else -> Stats(per = v)
            }
            one(a, main) + one(b, budget - main)
        }
        return Gear(
            key = key(t, slot), slot = slot, shape = shape, level = tierLevel(t),
            price = 150.0 + 60.0 * t + 6.0 * t * t, stats = stats,
            primary = p.toInt(), secondary = s.toInt(), name = 0,
            glow = rarity != Rarity.RARE, rarity = rarity, tier = t, prefix = (t * 7 + slot.ordinal * 3) % PREFIXES,
        )
    }
}
