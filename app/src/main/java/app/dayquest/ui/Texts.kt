package app.dayquest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.dayquest.R
import app.dayquest.game.Catalog
import app.dayquest.game.Difficulty
import app.dayquest.game.Food
import app.dayquest.game.Potion
import app.dayquest.game.Slot
import app.dayquest.game.Species
import app.dayquest.pixel.Sprite
import app.dayquest.pixel.Sprites
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** Gold reads as a whole number once it's big; small amounts keep one decimal ("2.4"). */
fun goldText(g: Double): String {
    val v = kotlin.math.round(g * 10) / 10
    return if (kotlin.math.abs(v) >= 100 || v == floor(v)) floor(v + 1e-9).toLong().toString()
    else String.format(Locale.US, "%.1f", v)
}

fun num(v: Double): String = v.roundToInt().toString()

/** "+1.5" style for small gains that would round to zero. */
fun gainText(v: Double): String = if (v >= 1) v.roundToInt().toString() else String.format(Locale.US, "%.1f", v)

@Composable
fun difficultyLabel(d: Difficulty) = stringResource(
    when (d) {
        Difficulty.TRIVIAL -> R.string.diff_trivial
        Difficulty.EASY -> R.string.diff_easy
        Difficulty.MEDIUM -> R.string.diff_medium
        Difficulty.HARD -> R.string.diff_hard
    }
)

@Composable
fun slotLabel(s: Slot) = stringResource(
    when (s) {
        Slot.WEAPON -> R.string.slot_weapon
        Slot.ARMOR -> R.string.slot_armor
        Slot.HEAD -> R.string.slot_head
        Slot.SHIELD -> R.string.slot_shield
    }
)

@Composable
fun petName(key: String): String {
    val (s, p) = Catalog.petParts(key) ?: return key
    return stringResource(R.string.pet_name, stringResource(s.label), stringResource(p.label))
}

/** Human name of an inventory key: "egg:fox", "potion:frost", "food:berry". */
@Composable
fun itemName(key: String): String {
    val id = key.substringAfter(':')
    return when {
        key.startsWith("egg:") -> Species.of(id)?.let { stringResource(R.string.egg_name, stringResource(it.label)) }
        key.startsWith("potion:") -> Potion.of(id)?.let { stringResource(R.string.potion_name, stringResource(it.label)) }
        key.startsWith("food:") -> Food.of(id)?.let { stringResource(it.label) }
        key == app.dayquest.game.Rules.SADDLE -> stringResource(R.string.saddle)
        else -> null
    } ?: key
}

fun itemSprite(key: String): Sprite? {
    val id = key.substringAfter(':')
    return when {
        key.startsWith("egg:") -> Species.of(id)?.let(Sprites::egg)
        key.startsWith("potion:") -> Potion.of(id)?.let { Sprites.potion(it) }
        key.startsWith("food:") -> Food.of(id)?.let(Sprites::food)
        key == app.dayquest.game.Rules.SADDLE -> app.dayquest.pixel.Badges.saddle
        else -> null
    }
}

/** What a skill does, with this hero's numbers filled in. */
@Composable
fun skillText(skill: app.dayquest.game.Skill, s: app.dayquest.game.Stats): String = when (skill) {
    app.dayquest.game.Skill.SMASH -> stringResource(skill.text, gainText(app.dayquest.game.Classes.smash(s)))
    app.dayquest.game.Skill.FIREBALL -> stringResource(skill.text, gainText(app.dayquest.game.Classes.fireball(s)), gainText(app.dayquest.game.Classes.fireballXp(s)))
    app.dayquest.game.Skill.PICKPOCKET -> stringResource(skill.text, goldText(app.dayquest.game.Classes.pickpocket(s)))
    app.dayquest.game.Skill.HEAL -> stringResource(skill.text, gainText(app.dayquest.game.Classes.heal(s)))
    else -> stringResource(skill.text)
}

@Composable
fun dayLabel(day: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()).format(LocalDate.ofEpochDay(day))

fun timeLabel(minutes: Int): String = String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

/** Non-composable twin of [itemName], for snackbars built outside composition. */
fun android.content.Context.itemLabel(key: String): String {
    val id = key.substringAfter(':')
    return when {
        key.startsWith("egg:") -> Species.of(id)?.let { getString(R.string.egg_name, getString(it.label)) }
        key.startsWith("potion:") -> Potion.of(id)?.let { getString(R.string.potion_name, getString(it.label)) }
        key.startsWith("food:") -> Food.of(id)?.let { getString(it.label) }
        key == app.dayquest.game.Rules.SADDLE -> getString(R.string.saddle)
        else -> null
    } ?: key
}

/** Hand-named gear uses its string; forged gear is "prefix + noun", with Russian adjectives agreeing in gender. */
@Composable
fun gearName(g: app.dayquest.game.Gear): String {
    if (g.name != 0) return stringResource(g.name)
    val prefix = androidx.compose.ui.res.stringArrayResource(R.array.forge_prefix)[g.prefix].split('|')
    val noun = stringResource(g.shape.noun).split('|')
    val form = when (noun.getOrNull(1)) {
        "f" -> 1
        "n" -> 2
        "p" -> 3
        else -> 0
    }
    val adj = prefix.getOrElse(form) { prefix[0] }.replaceFirstChar { it.uppercase() }
    return "$adj ${noun[0]}"
}

@Composable
fun bossName(b: app.dayquest.game.Boss): String {
    val base = when (b.kind) {
        app.dayquest.game.BossKind.WEEKLY -> stringResource(
            R.string.weekly_name,
            androidx.compose.ui.res.stringArrayResource(R.array.weekly_adj)[b.adjective],
            stringResource(b.shape.noun),
        )
        else -> stringResource(b.name)
    }
    return when {
        b.rank <= 0 -> base
        b.rank == app.dayquest.game.Catalog.ELITE -> stringResource(R.string.rank_elite, base)
        b.rank == app.dayquest.game.Catalog.ANCIENT -> stringResource(R.string.rank_ancient, base)
        else -> stringResource(R.string.rank_mythic, base, b.rank - 2)
    }
}

/** The hour the player's day starts at (for the logical "today"). */
@Composable
fun rememberDayStart(): Int {
    val c = androidx.compose.ui.platform.LocalContext.current.let { (it.applicationContext as app.dayquest.DayquestApp).container }
    val s by androidx.compose.runtime.remember { c.settings.settings }.collectAsState(initial = null)
    return s?.dayStart ?: 0
}

/** The real-calendar season, flipped for the southern hemisphere when the player asked. */
@Composable
fun rememberSeason(): app.dayquest.game.Season {
    val c = androidx.compose.ui.platform.LocalContext.current.let { (it.applicationContext as app.dayquest.DayquestApp).container }
    val southern by androidx.compose.runtime.remember { c.settings.settings }.collectAsState(initial = null)
    return app.dayquest.game.Season.of(LocalDate.now(), southern?.southern ?: false)
}

/** Non-composable twin of [petName]. */
fun android.content.Context.petLabel(key: String): String {
    val (s, p) = Catalog.petParts(key) ?: return key
    return getString(R.string.pet_name, getString(s.label), getString(p.label))
}
