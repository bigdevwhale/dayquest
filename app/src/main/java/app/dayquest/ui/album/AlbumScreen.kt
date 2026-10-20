package app.dayquest.ui.album

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Moment
import app.dayquest.data.Pet
import app.dayquest.game.Catalog
import app.dayquest.game.MomentKind
import app.dayquest.game.Season
import app.dayquest.game.Snap
import app.dayquest.game.Weather
import app.dayquest.pixel.Look
import app.dayquest.pixel.SceneRenderer
import app.dayquest.pixel.SceneState
import app.dayquest.ui.bossName
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.dayLabel
import app.dayquest.ui.pets.petTitle
import app.dayquest.ui.pets.rememberPets
import app.dayquest.ui.pets.titleName
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import java.io.File

private const val VW = 172
private const val VH = 72

/** The scene of a moment, drawn again from its line: a few frames in, so clouds and sparkles have settled. */
fun snapScene(s: Snap): SceneState = SceneState(
    Catalog.scene(s.scene).kind, s.hour, Look(s.skin, s.hairStyle, s.hairColor, s.weapon, s.armor, s.head, s.shield),
    s.pet, s.grown, Catalog.boss(s.boss), false, Season.byKey(s.season), mount = s.mount,
    petForm = s.form, petWear = s.wear, weather = runCatching { Weather.valueOf(s.weather) }.getOrDefault(Weather.CLEAR),
)

fun snapBitmap(s: Snap): Bitmap {
    val r = SceneRenderer(VW, VH)
    val state = snapScene(s)
    repeat(20) { r.render(state, 10_000L + it * 66) }
    return Bitmap.createBitmap(VW, VH, Bitmap.Config.ARGB_8888).also { it.setPixels(r.buffer.px, 0, VW, 0, 0, VW, VH) }
}

@Composable
fun AlbumScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val moments by remember { c.db.game().observeMoments() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val pets = rememberPets()
    var open by remember { mutableStateOf<Moment?>(null) }
    Column(Modifier.fillMaxSize().background(D.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.album), onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item {
                Text(stringResource(R.string.album_text), style = DType.Muted, modifier = Modifier.padding(bottom = 10.dp))
                if (moments.isEmpty()) Text(stringResource(R.string.album_empty), style = DType.Sub, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(moments.chunked(2), key = { it.first().id }) { pair ->
                Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { m -> MomentCard(m, pets, Modifier.weight(1f)) { open = m } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    open?.let { m -> MomentView(m, pets) { open = null } }
}

@Composable
private fun Picture(m: Moment, modifier: Modifier = Modifier) {
    val img: ImageBitmap? = remember(m.id) { Snap.decode(m.scene)?.let { snapBitmap(it).asImageBitmap() } }
    Canvas(modifier.fillMaxWidth().aspectRatio(VW / VH.toFloat()).clip(D.RSm).background(D.c.surface2)) {
        img?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.None) }
    }
}

/** What a moment was, in words. */
@Composable
fun caption(m: Moment, pets: List<Pet>): String {
    val pet = pets.firstOrNull { it.key == m.subject }
    return when (runCatching { MomentKind.valueOf(m.kind) }.getOrNull()) {
        MomentKind.HATCH -> stringResource(R.string.m_hatch, petTitle(pet))
        MomentKind.GROWN -> stringResource(R.string.m_grown, petTitle(pet))
        MomentKind.VICTORY -> stringResource(R.string.m_victory, Snap.decode(m.scene)?.let { Catalog.boss(it.boss) }?.let { bossName(it) } ?: "")
        MomentKind.PERFECT -> stringResource(R.string.m_perfect)
        MomentKind.LEVEL -> stringResource(R.string.m_level, m.subject)
        MomentKind.DUNGEON -> stringResource(R.string.m_dungeon, m.subject)
        MomentKind.RIDE -> stringResource(R.string.m_ride, petTitle(pet))
        MomentKind.FRIEND -> stringResource(R.string.m_friend, petTitle(pet))
        MomentKind.EVOLVE -> stringResource(R.string.m_evolve, petTitle(pet))
        MomentKind.COLLECTION -> stringResource(R.string.m_collection, titleName(m.subject))
        null -> ""
    }
}

@Composable
private fun MomentCard(m: Moment, pets: List<Pet>, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd).clickable(onClick = onClick).padding(8.dp)) {
        Picture(m)
        Gap(6)
        Text(caption(m, pets), style = DType.Strong, maxLines = 2)
        Text(dayLabel(m.day), style = DType.Small)
    }
}

@Composable
private fun MomentView(m: Moment, pets: List<Pet>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val text = caption(m, pets)
    val date = dayLabel(m.day)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        text = {
            Column(Modifier.fillMaxWidth()) {
                Picture(m)
                Gap(12)
                Text(text, style = DType.Title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(date, style = DType.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Gap(16)
                PrimaryButton(stringResource(R.string.album_share), { Snap.decode(m.scene)?.let { share(ctx, it, "$text · $date") } }, Modifier.fillMaxWidth().navigationBarsPadding())
                Gap(6)
                SoftButton(stringResource(R.string.close), onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}

/** Sends the moment's picture, scaled up crisply, with its caption. */
private fun share(context: Context, s: Snap, caption: String) {
    val small = snapBitmap(s)
    val big = Bitmap.createScaledBitmap(small, VW * 6, VH * 6, false)
    val dir = File(context.cacheDir, "transfer").apply { mkdirs() }
    val file = File(dir, "moment.png")
    file.outputStream().use { big.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, "$caption · Dayquest")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = android.content.ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
