package app.dayquest.game

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** What a friend sees of your hero: looks, level and a few proud numbers. No tasks, nothing private. */
data class HeroCard(
    val uid: String,
    val name: String,
    val level: Int,
    val heroClass: String,
    val skin: Int,
    val hairStyle: Int,
    val hairColor: Int,
    val weapon: String,
    val armor: String,
    val head: String,
    val shield: String,
    val pet: String,
    val petGrown: Boolean,
    val mount: String,
    val bestStreak: Int,
    val bosses: Int,
    val pets: Int,
    val achievements: Int,
    val dungeons: Int,
    /** The logical day the card was made: fresh cards give the party bonus. */
    val day: Long,
)

/**
 * The tavern without a server: heroes travel as a one-line code ("DQH1:…") through any messenger.
 * Friends whose card is fresh (made within a week) give a small party bonus.
 */
object Party {
    const val PREFIX = "DQH1:"
    const val FRESH_DAYS = 7
    const val PER_FRIEND = 0.02
    const val MAX_BONUS_FRIENDS = 5

    /** Fields are joined by the ASCII unit separator, which never appears in a typed name. */
    private const val SEP = '\u001F'

    fun encode(c: HeroCard): String {
        val fields = listOf(
            c.uid, c.name.replace(SEP, ' '), c.level, c.heroClass, c.skin, c.hairStyle, c.hairColor,
            c.weapon, c.armor, c.head, c.shield, c.pet, if (c.petGrown) 1 else 0, c.mount,
            c.bestStreak, c.bosses, c.pets, c.achievements, c.dungeons, c.day,
        ).joinToString(SEP.toString())
        val bytes = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(fields.toByteArray()) } }.toByteArray()
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Null when the text is not a hero code (a backup code, a typo, half a message...). */
    fun decode(text: String): HeroCard? = runCatching {
        val t = text.trim()
        val at = t.indexOf(PREFIX)
        if (at < 0) return null
        val raw = t.substring(at + PREFIX.length).takeWhile { !it.isWhitespace() }
        val bytes = Base64.getUrlDecoder().decode(raw)
        val f = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes().decodeToString() }.split(SEP)
        if (f.size < 20 || f[0].isBlank()) return null
        HeroCard(
            uid = f[0], name = f[1].take(24), level = f[2].toInt(), heroClass = f[3],
            skin = f[4].toInt(), hairStyle = f[5].toInt(), hairColor = f[6].toInt(),
            weapon = f[7], armor = f[8], head = f[9], shield = f[10], pet = f[11], petGrown = f[12] == "1", mount = f[13],
            bestStreak = f[14].toInt(), bosses = f[15].toInt(), pets = f[16].toInt(), achievements = f[17].toInt(), dungeons = f[18].toInt(),
            day = f[19].toLong(),
        )
    }.getOrNull()

    fun isFresh(c: HeroCard, today: Long) = today - c.day in 0..FRESH_DAYS.toLong()

    /** Extra XP and gold from friends with fresh cards: +2% each, up to five. */
    fun bonus(cards: List<HeroCard>, today: Long): Double =
        PER_FRIEND * cards.count { isFresh(it, today) }.coerceAtMost(MAX_BONUS_FRIENDS)
}

/** What happened to a pasted hero code. */
enum class FriendResult { ADDED, UPDATED, BAD, SELF }
