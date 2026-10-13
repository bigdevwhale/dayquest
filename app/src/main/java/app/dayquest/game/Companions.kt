package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R
import app.dayquest.data.Pet
import kotlin.random.Random

/** What a companion helps with. [full] is the bonus of a fully grown pet; a young one gives half. */
enum class Perk(val full: Double, @StringRes val text: Int) {
    /** More gold from every task. */
    GOLD(0.10, R.string.perk_gold),
    /** More XP from every task. */
    XP(0.10, R.string.perk_xp),
    /** A better chance of finding an item. */
    DROP(0.06, R.string.perk_drop),
    /** Missed dailies hurt less. */
    GUARD(0.20, R.string.perk_guard),
    /** Each finished task heals a little (in HP). */
    HEAL(1.0, R.string.perk_heal),
    /** Harder hits on the boss. */
    BOSS(0.15, R.string.perk_boss),
    /** A better chance of a critical hit. */
    CRIT(0.08, R.string.perk_crit),
}

/** What the current companion adds; all zero without one. */
data class Boost(
    val gold: Double = 0.0,
    val xp: Double = 0.0,
    val drop: Double = 0.0,
    val guard: Double = 0.0,
    val heal: Double = 0.0,
    val boss: Double = 0.0,
    val crit: Double = 0.0,
) {
    operator fun plus(o: Boost) = Boost(gold + o.gold, xp + o.xp, drop + o.drop, guard + o.guard, heal + o.heal, boss + o.boss, crit + o.crit)
}

object Companions {
    fun perk(s: Species): Perk = when (s) {
        Species.FOX -> Perk.GOLD
        Species.CAT -> Perk.DROP
        Species.OWL -> Perk.XP
        Species.SLIME -> Perk.GUARD
        Species.BUNNY -> Perk.HEAL
        Species.WOLF -> Perk.BOSS
        Species.TURTLE -> Perk.GUARD
        Species.DRAGON -> Perk.CRIT
    }

    /** The boss-only pets are a little stronger. */
    private fun rare(s: Species) = if (s.questOnly) 1.5 else 1.0

    /** How much of the perk a pet gives: half while young, all of it once fully grown. */
    fun amount(pet: Pet): Double {
        val s = Catalog.petParts(pet.key)?.first ?: return 0.0
        return perk(s).full * rare(s) * if (pet.growth >= 100) 1.0 else 0.5
    }

    /** A saddled pet carries the hero and gives its perk in full, on top of the companion's. */
    fun mountBoost(pet: Pet?): Boost = if (pet == null || !pet.mount) Boost() else boost(pet.copy(growth = 100))

    fun boost(pet: Pet?): Boost {
        if (pet == null) return Boost()
        val s = Catalog.petParts(pet.key)?.first ?: return Boost()
        val a = amount(pet)
        return when (perk(s)) {
            Perk.GOLD -> Boost(gold = a)
            Perk.XP -> Boost(xp = a)
            Perk.DROP -> Boost(drop = a)
            Perk.GUARD -> Boost(guard = a)
            Perk.HEAL -> Boost(heal = a)
            Perk.BOSS -> Boost(boss = a)
            Perk.CRIT -> Boost(crit = a)
        }
    }

    /** The chance the companion brings something home overnight. */
    fun findChance(pet: Pet) = if (pet.growth >= 100) 0.6 else 0.3

    /** What the companion found overnight, or "": often its favourite food, sometimes any. */
    fun forage(pet: Pet?, random: Random): String {
        if (pet == null || random.nextDouble() >= findChance(pet)) return ""
        val s = Catalog.petParts(pet.key)?.first ?: return ""
        val food = if (random.nextInt(2) == 0) s.favorite else Food.entries[random.nextInt(Food.entries.size)]
        return "food:" + food.key
    }
}
