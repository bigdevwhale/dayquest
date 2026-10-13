package app.dayquest.ui.tavern

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.game.Catalog
import app.dayquest.game.FriendResult
import app.dayquest.game.HeroCard
import app.dayquest.game.HeroClass
import app.dayquest.game.Party
import app.dayquest.pixel.Badges
import app.dayquest.pixel.Look
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.Tip
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.dayLabel
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun HeroCard.look() = Look(skin, hairStyle, hairColor, weapon, armor, head, shield)

@Composable
/** The tavern, as the hero screen's tab: your code, the party bonus and your friends' heroes. */
fun TavernContent() {
    val context = LocalContext.current
    val c = context.container
    val fb = LocalFeedback.current
    val friends by remember { c.db.game().observeFriends() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val cards = friends.mapNotNull { f -> Party.decode(f.code) }
    var code by remember { mutableStateOf("") }
    var today by remember { mutableLongStateOf(0L) }
    var bonus by remember { mutableDoubleStateOf(0.0) }
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<HeroCard?>(null) }
    LaunchedEffect(Unit) {
        code = Party.encode(c.game.heroCard())
        today = c.game.today()
    }
    LaunchedEffect(friends) { bonus = c.game.partyBonus() }
    val copied = stringResource(R.string.copied)

    Column {
        Tip("tavern", stringResource(R.string.tip_tavern), Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelImage(Badges.mug, pixel = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.tavern_text), style = DType.Sub, modifier = Modifier.weight(1f))
        }
        if (bonus > 0) {
            Gap(10)
            Box(Modifier.clip(D.RXs).background(D.c.xpSoft).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.tavern_bonus, "${kotlin.math.round(bonus * 100).toInt()}%"), style = DType.Small.copy(color = D.c.ink))
            }
        }
        SectionTitle(stringResource(R.string.tavern_my_code))
        DCard {
            Text(code, style = DType.Small.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Gap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton(stringResource(R.string.tavern_copy), {
                    copy(context, code); fb.tap(); fb.show(copied)
                }, Modifier.weight(1f))
                SoftButton(stringResource(R.string.tavern_send), {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, code), null))
                }, Modifier.weight(1f))
            }
            Gap(8)
            SoftButton(stringResource(R.string.share_hero), { c.scope.launch { ShareCard.share(context, c) } }, Modifier.fillMaxWidth())
        }
        SectionTitle(stringResource(R.string.friends)) {
            Text("${cards.size}", style = DType.Number.copy(color = D.c.muted))
        }
        PrimaryButton(stringResource(R.string.tavern_add), { adding = true }, Modifier.fillMaxWidth())
        Gap(8)
        if (cards.isEmpty()) Text(stringResource(R.string.tavern_empty), style = DType.Sub, modifier = Modifier.padding(vertical = 8.dp))
        cards.sortedByDescending { it.day }.forEach { f -> FriendCard(f, today) { removing = f } }
    }

    if (adding) PasteDialog(onDismiss = { adding = false }) { text ->
        c.scope.launch {
            val (result, card) = c.game.addFriend(text)
            withContext(Dispatchers.Main) {
                when (result) {
                    FriendResult.ADDED -> { fb.thud(); app.dayquest.ui.Sfx.play(app.dayquest.ui.Sound.CARD); fb.show(context.getString(R.string.tavern_added, card!!.name)); adding = false }
                    FriendResult.UPDATED -> { fb.tap(); fb.show(context.getString(R.string.tavern_updated, card!!.name)); adding = false }
                    FriendResult.SELF -> fb.show(context.getString(R.string.tavern_self))
                    FriendResult.BAD -> fb.show(context.getString(R.string.tavern_bad))
                }
            }
        }
    }
    removing?.let { f ->
        ConfirmDialog(
            stringResource(R.string.tavern_remove_q, f.name), "", stringResource(R.string.tavern_remove),
            onConfirm = { c.launch { c.db.game().deleteFriend(f.uid) } }, onDismiss = { removing = null }, danger = true,
        )
    }
}

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Dayquest", text))
}

private fun clipboard(context: Context): String =
    context.getSystemService(ClipboardManager::class.java).primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()

@Composable
private fun PasteDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val context = LocalContext.current
    // A hero code already on the clipboard fills itself in.
    var text by remember { mutableStateOf(clipboard(context).takeIf { Party.PREFIX in it }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        title = { Text(stringResource(R.string.tavern_add), style = DType.Heading) },
        text = {
            Column {
                DTextField(text, { text = it }, placeholder = stringResource(R.string.tavern_paste_hint), minLines = 3)
                Gap(16)
                PrimaryButton(stringResource(R.string.add), { onAdd(text) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
            }
        },
    )
}

@Composable
private fun FriendCard(f: HeroCard, today: Long, onRemove: () -> Unit) {
    val fresh = Party.isFresh(f, today)
    Column(
        Modifier.padding(vertical = 6.dp).fillMaxWidth().clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(width = 96.dp, height = 84.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.BottomCenter) {
                val rider = Sprites.hero(f.look())
                val mount = Catalog.petParts(f.mount)?.let { (s, p) -> Sprites.mount(s, p) }
                if (mount != null) {
                    // The friend rides: head and shoulders above the beast, as in the scene.
                    PixelImage(rider, pixel = 1.5.dp, modifier = Modifier.padding(bottom = (app.dayquest.pixel.riderRise(mount, rider) * 1.5f).dp))
                    PixelImage(mount, pixel = 1.5.dp)
                } else PixelImage(rider, pixel = 2.5.dp)
                Catalog.petParts(f.pet)?.let { (s, p) ->
                    Box(Modifier.align(Alignment.BottomEnd)) { PixelImage(Sprites.pet(s, p, f.petGrown), pixel = 1.5.dp) }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = DType.Heading.copy(fontSize = 21.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val cls = HeroClass.of(f.heroClass)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.lv, f.level), style = DType.Pixel.copy(color = D.c.accent))
                    if (cls != null) {
                        Spacer(Modifier.width(8.dp))
                        PixelImage(Badges.emblem(cls), pixel = 1.5.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(cls.label), style = DType.Small)
                    }
                }
                Gap(4)
                Text(
                    if (fresh) stringResource(R.string.tavern_fresh, dayLabel(f.day)) else stringResource(R.string.tavern_stale),
                    style = DType.Small.copy(color = if (fresh) D.c.good else D.c.faint),
                )
            }
        }
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Stat(f.bestStreak, stringResource(R.string.share_streak), Modifier.weight(1f))
            Stat(f.bosses, stringResource(R.string.share_bosses), Modifier.weight(1f))
            Stat(f.pets, stringResource(R.string.share_pets), Modifier.weight(1f))
            Text(
                stringResource(R.string.tavern_remove), style = DType.Small.copy(color = D.c.hp),
                modifier = Modifier.clip(D.RXs).clickable(onClick = onRemove).padding(6.dp),
            )
        }
    }
}

@Composable
private fun Stat(v: Int, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(v.toString(), style = DType.Number.copy(fontSize = 17.sp))
        Text(label, style = DType.Small.copy(fontSize = 11.sp), maxLines = 1)
    }
}
