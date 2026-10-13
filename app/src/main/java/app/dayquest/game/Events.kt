package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R

/** What a morning card hands out the moment it is opened, or as a bounty. */
enum class Gift { NONE, EGG, POTION, FOODS }

enum class EventIcon { COIN, HEART, STAR, GEM, FLAME, CHEST, HEALTH, EGG, POTION, FOOD, MEDAL }

/**
 * The morning card: one small twist on the day's rules. Multipliers default to 1 (no change).
 * [diff] boosts rewards of one task difficulty by [diffMult]; [first] boosts the day's first task;
 * a [bounty] pays [bountyGold] and [bountyGift] once that many tasks are done that day.
 */
enum class DayEvent(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val icon: EventIcon,
    val xp: Double = 1.0,
    val gold: Double = 1.0,
    val habits: Double = 1.0,
    val dailies: Double = 1.0,
    val todos: Double = 1.0,
    val diff: Difficulty? = null,
    val diffMult: Double = 1.0,
    val first: Double = 1.0,
    val streak: Double = 1.0,
    val boss: Double = 1.0,
    val crit: Double = 0.0,
    val drop: Double = 0.0,
    val dropCap: Int = 0,
    val jackpot: Double = 0.0,
    val hurt: Double = 1.0,
    val potion: Double = 1.0,
    val feed: Double = 1.0,
    val giftGold: Double = 0.0,
    val heal: Double = 0.0,
    val gift: Gift = Gift.NONE,
    val bounty: Int = 0,
    val bountyGold: Double = 0.0,
    val bountyGift: Gift = Gift.NONE,
) {
    MERCHANT(R.string.ev_merchant_t, R.string.ev_merchant_d, EventIcon.HEALTH, potion = 0.5),
    GOLD_RUSH(R.string.ev_gold_rush_t, R.string.ev_gold_rush_d, EventIcon.COIN, gold = 2.0),
    FULL_MOON(R.string.ev_full_moon_t, R.string.ev_full_moon_d, EventIcon.STAR, xp = 1.5),
    GOBLIN(R.string.ev_goblin_t, R.string.ev_goblin_d, EventIcon.COIN, giftGold = -5.0, bounty = 3, bountyGold = 15.0),
    WHETSTONE(R.string.ev_whetstone_t, R.string.ev_whetstone_d, EventIcon.FLAME, boss = 1.5),
    CLOVER(R.string.ev_clover_t, R.string.ev_clover_d, EventIcon.GEM, drop = 0.15, dropCap = 3),
    EAGLE_EYE(R.string.ev_eagle_eye_t, R.string.ev_eagle_eye_d, EventIcon.STAR, crit = 0.15),
    COZY_RAIN(R.string.ev_cozy_rain_t, R.string.ev_cozy_rain_d, EventIcon.HEART, dailies = 1.5),
    FAIR(R.string.ev_fair_t, R.string.ev_fair_d, EventIcon.COIN, gold = 1.5),
    HEALER(R.string.ev_healer_t, R.string.ev_healer_d, EventIcon.HEART, heal = 15.0),
    GUARDIAN(R.string.ev_guardian_t, R.string.ev_guardian_d, EventIcon.MEDAL, hurt = 0.5),
    ECHO(R.string.ev_echo_t, R.string.ev_echo_d, EventIcon.GEM, habits = 2.0),
    TREASURE_MAP(R.string.ev_treasure_map_t, R.string.ev_treasure_map_d, EventIcon.CHEST, bounty = 5, bountyGold = 25.0),
    PICNIC(R.string.ev_picnic_t, R.string.ev_picnic_d, EventIcon.FOOD, feed = 2.0),
    SMALL_STEPS(R.string.ev_small_steps_t, R.string.ev_small_steps_d, EventIcon.STAR, diff = Difficulty.TRIVIAL, diffMult = 3.0),
    DRAGON_FIRE(R.string.ev_dragon_fire_t, R.string.ev_dragon_fire_d, EventIcon.FLAME, diff = Difficulty.HARD, diffMult = 1.5),
    BONFIRE(R.string.ev_bonfire_t, R.string.ev_bonfire_d, EventIcon.FLAME, streak = 2.0),
    BARD(R.string.ev_bard_t, R.string.ev_bard_d, EventIcon.STAR, first = 3.0),
    STORK(R.string.ev_stork_t, R.string.ev_stork_d, EventIcon.EGG, gift = Gift.EGG),
    ALCHEMIST(R.string.ev_alchemist_t, R.string.ev_alchemist_d, EventIcon.POTION, gift = Gift.POTION),
    BASKET(R.string.ev_basket_t, R.string.ev_basket_d, EventIcon.FOOD, gift = Gift.FOODS),
    FOG(R.string.ev_fog_t, R.string.ev_fog_d, EventIcon.GEM, xp = 1.4, gold = 0.8),
    BLOOD_MOON(R.string.ev_blood_moon_t, R.string.ev_blood_moon_d, EventIcon.FLAME, boss = 2.0, hurt = 1.5),
    INK_QUILL(R.string.ev_ink_quill_t, R.string.ev_ink_quill_d, EventIcon.MEDAL, todos = 2.0),
    EARLY_BIRD(R.string.ev_early_bird_t, R.string.ev_early_bird_d, EventIcon.EGG, bounty = 3, bountyGift = Gift.EGG),
    LUCKY_COIN(R.string.ev_lucky_coin_t, R.string.ev_lucky_coin_d, EventIcon.COIN, jackpot = 0.15),
    MENTOR(R.string.ev_mentor_t, R.string.ev_mentor_d, EventIcon.MEDAL, xp = 1.25, gold = 1.25),
    COIN_RAIN(R.string.ev_coin_rain_t, R.string.ev_coin_rain_d, EventIcon.COIN, giftGold = 10.0),
    CHAMPION(R.string.ev_champion_t, R.string.ev_champion_d, EventIcon.CHEST, bounty = 8, bountyGold = 40.0, bountyGift = Gift.POTION),
    STEADY(R.string.ev_steady_t, R.string.ev_steady_d, EventIcon.HEART, diff = Difficulty.MEDIUM, diffMult = 1.5);

    /** How much one finished task pays on top of the usual, before the global [xp] / [gold]. */
    fun reward(kind: app.dayquest.data.LogKind, d: Difficulty, isFirst: Boolean): Double {
        val byType = when (kind) {
            app.dayquest.data.LogKind.UP -> habits
            app.dayquest.data.LogKind.DAILY -> dailies
            app.dayquest.data.LogKind.TODO -> todos
            else -> 1.0
        }
        return byType * (if (d == diff) diffMult else 1.0) * (if (isFirst) first else 1.0)
    }

    companion object {
        /**
         * A fixed shuffle walked one card a day: any 30 days in a row show 30 different cards,
         * and the same day always shows the same card (even after a reinstall).
         */
        private val ORDER: List<DayEvent> = entries.shuffled(kotlin.random.Random(20261013))

        fun of(day: Long): DayEvent = ORDER[Math.floorMod(day, ORDER.size.toLong()).toInt()]
    }
}
