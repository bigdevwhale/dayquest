package app.dayquest.game

import androidx.annotation.StringRes
import app.dayquest.R
import app.dayquest.data.Pet
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Every pet is born with a character. It comes from the pet's key and the moment it hatched,
 * so nothing is stored and a backup brings back the same pet.
 */
enum class Trait(@StringRes val label: Int, @StringRes val text: Int) {
    /** Food grows it half as much again. */
    GLUTTON(R.string.trait_glutton, R.string.trait_glutton_text),
    /** Brings something home overnight more often. */
    SLEEPY(R.string.trait_sleepy, R.string.trait_sleepy_text),
    /** Hits bosses a little harder. */
    BRAVE(R.string.trait_brave, R.string.trait_brave_text),
    /** Finds items a little more often. */
    CURIOUS(R.string.trait_curious, R.string.trait_curious_text),
    /** A little more XP from every task. */
    CHEERFUL(R.string.trait_cheerful, R.string.trait_cheerful_text),
    /** Friendship grows half as fast again. */
    LOYAL(R.string.trait_loyal, R.string.trait_loyal_text);

    companion object {
        fun of(pet: Pet): Trait = of(pet.key, pet.hatchedAt)

        fun of(key: String, hatchedAt: Long): Trait {
            var h = hatchedAt * 31 + key.hashCode()
            h = h xor (h ushr 29)
            h *= -0x61c8864680b583ebL
            h = h xor (h ushr 32)
            return entries[Math.floorMod(h, entries.size.toLong()).toInt()]
        }
    }
}

/** Friendship: points grow into hearts (0..10). Three hearts teach a trick, six bring a gift, ten make a best friend. */
object Bond {
    const val MAX = 10
    const val TRICK = 3
    const val GIFT = 6

    /** Points for a day together with at least one finished task. */
    const val DAY = 10
    const val MOUNT_DAY = 5
    /** Once a day, for a pat. */
    const val PAT = 3
    const val FAVORITE = 6
    const val FOOD = 3
    /** A boss beaten together. */
    const val VICTORY = 15

    /** Points needed for [level] hearts: 15, 45, 90 … 825. */
    fun need(level: Int) = 15 * level * (level + 1) / 2

    fun level(points: Int) = (1..MAX).lastOrNull { points >= need(it) } ?: 0

    /** How far into the current heart, 0..1 (1 at the top). */
    fun progress(points: Int): Float {
        val l = level(points)
        if (l >= MAX) return 1f
        return (points - need(l)).toFloat() / (need(l + 1) - need(l))
    }

    fun gain(pet: Pet, base: Int) = if (Trait.of(pet) == Trait.LOYAL) (base * 1.5).roundToInt() else base

    fun best(pet: Pet) = level(pet.bond) >= MAX
}

/** A grown pet can evolve once, down one of two roads. The choice is for good. */
enum class Form(val key: String, val need: Int, @StringRes val label: Int, @StringRes val text: Int) {
    /** Days together (with at least one finished task) as the companion. */
    STAR("star", 30, R.string.form_star, R.string.form_star_text),
    /** Bosses beaten while it was the companion. */
    BATTLE("battle", 10, R.string.form_battle, R.string.form_battle_text);

    fun progress(pet: Pet) = when (this) {
        STAR -> pet.days
        BATTLE -> pet.wins
    }

    fun ready(pet: Pet) = pet.growth >= 100 && pet.form.isEmpty() && progress(pet) >= need

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

enum class WearSlot { HEAD, NECK, FACE }

/** Clothes for pets: one per slot, seen in the scene, the home and the stable. Owned once, worn by anyone. */
enum class PetWear(
    val key: String,
    val slot: WearSlot,
    val price: Double,
    val level: Int,
    @StringRes val label: Int,
    val season: Season? = null,
) {
    BOW("bow", WearSlot.HEAD, 40.0, 1, R.string.w_bow),
    FLOWER("flower", WearSlot.HEAD, 50.0, 1, R.string.w_flower),
    PARTY_HAT("party_hat", WearSlot.HEAD, 80.0, 3, R.string.w_party_hat),
    TOP_HAT("top_hat", WearSlot.HEAD, 150.0, 10, R.string.w_top_hat),
    WREATH("wreath", WearSlot.HEAD, 120.0, 1, R.string.w_wreath, Season.SPRING),
    STRAW_HAT("straw_hat", WearSlot.HEAD, 120.0, 1, R.string.w_straw_hat, Season.SUMMER),
    WITCH_HAT("witch_hat", WearSlot.HEAD, 120.0, 1, R.string.w_witch_hat, Season.AUTUMN),
    SANTA_HAT("santa_hat", WearSlot.HEAD, 120.0, 1, R.string.w_santa_hat, Season.WINTER),
    /** Not sold: every pet that reaches six hearts unlocks it. */
    RIBBON("ribbon", WearSlot.HEAD, 0.0, 1, R.string.w_ribbon),

    SCARF("scarf", WearSlot.NECK, 60.0, 1, R.string.w_scarf),
    BELL("bell", WearSlot.NECK, 70.0, 1, R.string.w_bell),
    BANDANA("bandana", WearSlot.NECK, 60.0, 2, R.string.w_bandana),
    BOWTIE("bowtie", WearSlot.NECK, 90.0, 5, R.string.w_bowtie),

    GLASSES("glasses", WearSlot.FACE, 100.0, 4, R.string.w_glasses),
    SUNGLASSES("sunglasses", WearSlot.FACE, 120.0, 6, R.string.w_sunglasses),
    MONOCLE("monocle", WearSlot.FACE, 200.0, 15, R.string.w_monocle);

    val gift get() = price <= 0.0

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }

        fun parse(csv: String): List<PetWear> = csv.split(',').mapNotNull { of(it) }

        fun encode(list: List<PetWear>) = list.joinToString(",") { it.key }

        /** Puts [w] on, replacing whatever was in its slot, or takes it off when already worn. */
        fun toggle(csv: String, w: PetWear): String {
            val now = parse(csv)
            return encode(if (w in now) now - w else now.filter { it.slot != w.slot } + w)
        }
    }
}

/** A collection to complete: every colour of one kind, every kind in one colour, or all of them. */
data class PetSet(
    val key: String,
    val members: List<String>,
    val gold: Double,
    /** A home decoration it unlocks, or "". */
    val decor: String = "",
    val species: Species? = null,
    val potion: Potion? = null,
)

object Collections {
    val all: List<PetSet> =
        Species.entries.map { s -> PetSet("sp:${s.key}", Potion.entries.map { Catalog.petKey(s, it) }, 200.0, "statue_${s.key}", species = s) } +
            Potion.entries.map { p -> PetSet("po:${p.key}", Species.entries.map { Catalog.petKey(it, p) }, 150.0, potion = p) } +
            PetSet("all", Species.entries.flatMap { s -> Potion.entries.map { Catalog.petKey(s, it) } }, 1000.0)

    fun of(key: String) = all.firstOrNull { it.key == key }

    fun count(set: PetSet, owned: Set<String>) = set.members.count { it in owned }

    fun complete(set: PetSet, owned: Set<String>) = set.members.all { it in owned }

    fun claimed(csv: String) = csv.split(',').filter { it.isNotEmpty() }.toSet()
}

/** The daily visit: opening the game on following days climbs a seven-day ladder of small gifts. */
data class VisitGift(val gold: Double, val items: List<String>)

object Visits {
    const val WEEK = 7

    /**
     * The streak after a visit [today]. A missed day doesn't wipe it: the weeks already
     * completed are kept and the ladder starts over from day one of the next week.
     */
    fun streak(lastDay: Long, streak: Int, today: Long): Int = when {
        lastDay == today -> streak
        lastDay == today - 1 -> streak + 1
        else -> streak / WEEK * WEEK + 1
    }

    /** Which rung of the ladder (1..7) a streak stands on. */
    fun step(streak: Int) = (streak - 1).mod(WEEK) + 1

    /** The same visit always gives the same gift, so the card can show it before it's opened. */
    fun gift(streak: Int, level: Int, day: Long): VisitGift {
        val r = Random(day * 31 + streak)
        fun food() = "food:" + Food.entries[r.nextInt(Food.entries.size)].key
        val base = when (step(streak)) {
            1 -> VisitGift(10.0 + level, emptyList())
            2 -> VisitGift(0.0, listOf(food(), food()))
            3 -> VisitGift(kotlin.math.round(15 + level * 1.5), emptyList())
            4 -> VisitGift(0.0, listOf("potion:" + Catalog.basicPotions().let { it[r.nextInt(it.size)] }.key))
            5 -> VisitGift(0.0, listOf(food(), food(), food()))
            6 -> VisitGift(25.0 + level * 2, emptyList())
            else -> VisitGift(0.0, listOf("egg:" + Catalog.basicEggs().let { it[r.nextInt(it.size)] }.key, "food:cake"))
        }
        val extra = buildList {
            if (streak % 30 == 0) add(listOf("potion:frost", "potion:shadow", "potion:ember")[r.nextInt(3)])
            if (streak % 100 == 0) add(listOf("egg:dragon", "egg:turtle")[r.nextInt(2)])
        }
        return base.copy(items = base.items + extra)
    }
}

/** What the companion's morning note is about. */
enum class NoteKind { FOUND, MISSED, BOND, RAIN, SNOW, PLAIN }

/** A note from the companion in the morning. [pick] chooses among the lines of its kind. */
data class PetNote(val pet: String, val kind: NoteKind, val item: String = "", val n: Int = 0, val pick: Int = 0)

/** A friendship milestone or a road that opened, for its own small celebration. */
enum class NewsKind { HEART, TRICK, GIFT, BEST, FORM }

data class PetNews(val pet: String, val kind: NewsKind, val level: Int = 0)

/** Moments kept in the album. */
enum class MomentKind { HATCH, GROWN, VICTORY, PERFECT, LEVEL, DUNGEON, RIDE, FRIEND, EVOLVE, COLLECTION }

/**
 * Everything needed to draw a moment's scene again years later, as one line of text: where the
 * hero stood, what they wore, who was with them and what they fought.
 */
data class Snap(
    val scene: String,
    val hour: Float,
    val season: String,
    val skin: Int,
    val hairStyle: Int,
    val hairColor: Int,
    val weapon: String,
    val armor: String,
    val head: String,
    val shield: String,
    val pet: String = "",
    val grown: Boolean = false,
    val form: String = "",
    val wear: String = "",
    val mount: String = "",
    val boss: String = "",
    val weather: String = "",
) {
    fun encode() = listOf(
        scene, hour.toString(), season, skin.toString(), hairStyle.toString(), hairColor.toString(),
        weapon, armor, head, shield, pet, if (grown) "1" else "0", form, wear, mount, boss, weather,
    ).joinToString("|")

    companion object {
        fun decode(s: String): Snap? {
            val p = s.split('|')
            if (p.size < 17) return null
            return Snap(
                p[0], p[1].toFloatOrNull() ?: 12f, p[2], p[3].toIntOrNull() ?: 1, p[4].toIntOrNull() ?: 0, p[5].toIntOrNull() ?: 1,
                p[6], p[7], p[8], p[9], p[10], p[11] == "1", p[12], p[13], p[14], p[15], p[16],
            )
        }
    }
}
