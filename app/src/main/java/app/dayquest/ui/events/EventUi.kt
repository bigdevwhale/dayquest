package app.dayquest.ui.events

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.container
import app.dayquest.game.Catalog
import app.dayquest.game.DayEvent
import app.dayquest.game.EventIcon
import app.dayquest.game.Food
import app.dayquest.game.Loot
import app.dayquest.pixel.Sprite
import app.dayquest.pixel.Sprites
import app.dayquest.ui.Sfx
import app.dayquest.ui.Sound
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.goldText
import app.dayquest.ui.itemName
import app.dayquest.ui.num
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun eventSprite(icon: EventIcon): Sprite = when (icon) {
    EventIcon.COIN -> Sprites.coin
    EventIcon.HEART -> Sprites.heart
    EventIcon.STAR -> Sprites.star
    EventIcon.GEM -> Sprites.gem
    EventIcon.FLAME -> Sprites.flame
    EventIcon.CHEST -> Sprites.chest
    EventIcon.HEALTH -> Sprites.healthPotion
    EventIcon.EGG -> Sprites.egg(Catalog.basicEggs().first())
    EventIcon.POTION -> Catalog.basicPotions().let { Sprites.potion(it.getOrElse(1) { _ -> it.first() }) }
    EventIcon.FOOD -> Sprites.food(Food.entries.first())
    EventIcon.MEDAL -> Sprites.medal(1)
}

/** Today's card on the task list, with the bounty's progress when it has one. */
@Composable
fun EventBanner(ev: DayEvent, done: Int, claimed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(D.RMd)
            .background(D.c.surface)
            .border(1.dp, D.c.accent.copy(alpha = 0.45f), D.RMd)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(D.RSm).background(D.c.accentSoft), contentAlignment = Alignment.Center) {
            PixelImage(eventSprite(ev.icon), pixel = 2.5.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(ev.title), style = DType.Strong.copy(fontSize = 15.sp), maxLines = 1)
            Text(stringResource(ev.text), style = DType.Muted, maxLines = 2)
        }
        if (ev.bounty > 0) {
            Spacer(Modifier.width(8.dp))
            val full = claimed || done >= ev.bounty
            Box(Modifier.clip(D.RXs).background(if (full) D.c.good.copy(alpha = 0.2f) else D.c.surface2).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    if (full) "✓" else "${done.coerceAtMost(ev.bounty)}/${ev.bounty}",
                    style = DType.Number.copy(fontSize = 14.sp, color = if (full) D.c.good else D.c.muted),
                )
            }
        }
    }
}

/** The morning card: flips in, says what today is about, hands out its gift on "Let's go". */
@Composable
fun EventCardDialog(ev: DayEvent, onOpen: () -> Unit) {
    val flip = remember { Animatable(90f) }
    LaunchedEffect(ev) {
        Sfx.play(Sound.CARD)
        flip.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
    }
    AlertDialog(
        onDismissRequest = {},
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        modifier = Modifier.graphicsLayer { rotationY = flip.value; cameraDistance = 12 * density },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.event_today), style = DType.Eyebrow)
                Gap(14)
                Box(Modifier.size(104.dp).clip(D.RMd).background(D.c.accentSoft).border(2.dp, D.c.accent.copy(alpha = 0.5f), D.RMd), contentAlignment = Alignment.Center) {
                    PixelImage(eventSprite(ev.icon), pixel = 6.dp)
                }
                Gap(16)
                Text(stringResource(ev.title), style = DType.Title, textAlign = TextAlign.Center)
                Gap(8)
                Text(stringResource(ev.text), style = DType.Sub, textAlign = TextAlign.Center)
                Gap(20)
                PrimaryButton(stringResource(R.string.event_go), onOpen, Modifier.fillMaxWidth())
            }
        },
    )
}

/** A chest that wobbles until tapped open, then shows what was inside. */
@Composable
fun ChestDialog(perfect: Boolean, chests: Int, onClose: (Loot?) -> Unit) {
    val c = LocalContext.current.container
    var loot by remember { mutableStateOf<Loot?>(null) }
    var busy by remember { mutableStateOf(false) }
    val wobble = rememberInfiniteTransition(label = "chest")
    val angle by wobble.animateFloat(-6f, 6f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "wobble")
    fun open() {
        if (busy) return
        busy = true
        c.scope.launch {
            val l = c.game.openChest()
            withContext(Dispatchers.Main) {
                if (l == null) onClose(null) else { Sfx.play(Sound.CHEST); loot = l }
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (loot != null) onClose(loot) },
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val l = loot
                PixelImage(
                    if (l == null) Sprites.chest else Sprites.chestOpen,
                    Modifier.graphicsLayer { rotationZ = if (l == null) angle else 0f },
                    pixel = 7.dp,
                )
                Gap(16)
                Text(
                    stringResource(if (l != null) R.string.chest_loot_title else if (perfect) R.string.chest_perfect_title else R.string.chest_title),
                    style = DType.Title, textAlign = TextAlign.Center,
                )
                Gap(8)
                if (l == null) {
                    Text(
                        if (perfect) stringResource(R.string.chest_perfect_text) else stringResource(R.string.chest_text, chests),
                        style = DType.Sub, textAlign = TextAlign.Center,
                    )
                    Gap(20)
                    PrimaryButton(stringResource(R.string.chest_open), { open() }, Modifier.fillMaxWidth())
                    TextButton(onClick = { onClose(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.chest_later), style = DType.Button, color = D.c.muted)
                    }
                } else {
                    val lines = listOf(stringResource(R.string.chest_gold, goldText(l.gold)), stringResource(R.string.chest_xp, num(l.xp))) + l.items.map { itemName(it) }
                    Text(lines.joinToString("\n"), style = DType.Sub.copy(color = D.c.ink), textAlign = TextAlign.Center)
                    Gap(20)
                    PrimaryButton(stringResource(R.string.hooray), { onClose(l) }, Modifier.fillMaxWidth())
                }
            }
        },
    )
}
