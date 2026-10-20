package app.dayquest.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.dayquest.R
import app.dayquest.data.Hero
import app.dayquest.game.NewsKind
import app.dayquest.game.NoteKind
import app.dayquest.game.PetNews
import app.dayquest.game.PetNote
import app.dayquest.game.Trait
import app.dayquest.pixel.Sprites
import app.dayquest.ui.MomentDialog
import app.dayquest.ui.Sfx
import app.dayquest.ui.Sound
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.itemName
import app.dayquest.ui.itemSprite
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType

private fun Trait.notes() = when (this) {
    Trait.GLUTTON -> R.array.note_glutton
    Trait.SLEEPY -> R.array.note_sleepy
    Trait.BRAVE -> R.array.note_brave
    Trait.CURIOUS -> R.array.note_curious
    Trait.CHEERFUL -> R.array.note_cheerful
    Trait.LOYAL -> R.array.note_loyal
}

/** The words of a morning note, in the companion's own voice (its character colours the quiet ones). */
@Composable
fun noteText(n: PetNote, trait: Trait?): String {
    fun Array<String>.pick(salt: Int = 0) = this[Math.floorMod(n.pick + salt * 7, size)]
    val main = when (n.kind) {
        NoteKind.FOUND -> stringArrayResource(R.array.note_found).pick().format(itemName(n.item), stringArrayResource(R.array.note_places).pick(1))
        NoteKind.MISSED -> pluralStringResource(R.plurals.note_missed, n.n, n.n)
        NoteKind.BOND -> stringArrayResource(R.array.note_bond).pick()
        NoteKind.RAIN -> stringArrayResource(R.array.note_rain).pick()
        NoteKind.SNOW -> stringArrayResource(R.array.note_snow).pick()
        NoteKind.PLAIN -> stringArrayResource(trait?.notes() ?: R.array.note_cheerful).pick()
    }
    val extra = if (n.item.isNotEmpty() && n.kind != NoteKind.FOUND) "\n\n" + stringResource(R.string.note_also_found, itemName(n.item)) else ""
    return main + extra
}

/** A little postcard from the companion, waiting in the morning. */
@Composable
fun NoteDialog(n: PetNote, hero: Hero, onDismiss: () -> Unit) {
    val pets = rememberPets()
    val pet = pets.firstOrNull { it.key == n.pet }
    LaunchedEffect(n) { Sfx.play(Sound.LETTER) }
    val paper = if (D.c.dark) Color(0xFF2E2840) else Color(0xFFFBF3E4)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.note_title, petTitle(pet)), style = DType.Eyebrow, textAlign = TextAlign.Center)
                Gap(12)
                Column(
                    Modifier.fillMaxWidth().clip(D.RMd).background(paper).border(2.dp, D.c.accent.copy(alpha = 0.35f), D.RMd).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (pet?.look()?.sprite() ?: Sprites.heart).let { PixelImage(it, pixel = 5.dp) }
                        if (n.item.isNotEmpty()) itemSprite(n.item)?.let { PixelImage(it, pixel = 4.dp) }
                    }
                    Gap(12)
                    Text(noteText(n, pet?.let { Trait.of(it) }), style = DType.Body, textAlign = TextAlign.Center)
                    Gap(12)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.note_sign, petTitle(pet)), style = DType.Pixel.copy(color = D.c.accent))
                        Box(Modifier.padding(start = 6.dp).size(14.dp), contentAlignment = Alignment.Center) { PixelImage(Sprites.heart, pixel = 1.5.dp) }
                    }
                }
                Gap(18)
                PrimaryButton(stringResource(R.string.note_ok), onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}

/** Three hearts, six hearts, ten hearts, or a road to a new form: each gets its own card. */
@Composable
fun PetNewsDialog(n: PetNews, onDismiss: () -> Unit) {
    val pets = rememberPets()
    val pet = pets.firstOrNull { it.key == n.pet }
    val name = petTitle(pet)
    LaunchedEffect(n) { Sfx.play(if (n.kind == NewsKind.FORM) Sound.SPELL else Sound.LEVEL) }
    val (title, text) = when (n.kind) {
        NewsKind.TRICK -> R.string.news_trick_title to stringResource(R.string.news_trick, name)
        NewsKind.GIFT -> R.string.news_gift_title to stringResource(R.string.news_gift, name)
        NewsKind.BEST -> R.string.news_best_title to stringResource(R.string.news_best, name)
        NewsKind.FORM -> R.string.news_form_title to stringResource(R.string.news_form, name)
        NewsKind.HEART -> R.string.news_trick_title to stringResource(R.string.news_heart, name, n.level)
    }
    MomentDialog(pet?.look()?.sprite() ?: Sprites.heart, stringResource(title), text, stringResource(R.string.hooray), pixel = 7, onDismiss = onDismiss)
}

/** Sprites for the visit ladder's rungs. */
fun visitIcon(step: Int) = when (step) {
    2, 5 -> Sprites.food(app.dayquest.game.Food.BERRY)
    4 -> Sprites.potion(app.dayquest.game.Potion.GOLDEN)
    7 -> Sprites.egg(app.dayquest.game.Species.FOX)
    else -> Sprites.coin
}

/** The seven-day ladder under the morning card: rungs behind, today's gift lit up, rungs ahead. */
@Composable
fun VisitStrip(streak: Int, gift: app.dayquest.game.VisitGift) {
    val step = app.dayquest.game.Visits.step(streak)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..app.dayquest.game.Visits.WEEK).forEach { i ->
            val now = i == step
            Box(
                Modifier.weight(1f).height(38.dp).clip(D.RSm)
                    .background(if (now) D.c.accentSoft else D.c.surface)
                    .border(if (now) 2.dp else 1.dp, if (now) D.c.accent else D.c.line, D.RSm),
                contentAlignment = Alignment.Center,
            ) {
                val icon = if (now) gift.items.firstOrNull()?.let { itemSprite(it) } ?: Sprites.coin else visitIcon(i)
                PixelImage(icon, pixel = 2.dp, alpha = if (i < step) 0.3f else if (now) 1f else 0.6f)
                if (i < step) Text("✓", style = DType.Strong.copy(color = D.c.good))
            }
        }
    }
    Gap(6)
    val parts = listOfNotNull(
        stringResource(R.string.visit_gold, app.dayquest.ui.goldText(gift.gold)).takeIf { gift.gold > 0 },
    ) + gift.items.map { itemName(it) }
    Text(pluralStringResource(R.plurals.visit_streak, streak, streak) + " · " + parts.joinToString(", "), style = DType.Small, textAlign = TextAlign.Center)
}
