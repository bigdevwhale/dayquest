package app.dayquest.ui.home

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.game.Decor
import app.dayquest.game.DecorType
import app.dayquest.game.HomeCatalog
import app.dayquest.game.Spot
import app.dayquest.pixel.RoomRenderer
import app.dayquest.pixel.RoomState
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PriceButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.goldText
import app.dayquest.ui.look
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.max

/** The room, rendered like the scene: small bitmap, whole-number scale, ~12 fps. */
@Composable
fun RoomView(state: RoomState, modifier: Modifier = Modifier, virtualHeight: Int = 84) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val scale = max(2, widthPx / 172)
        val vw = ceil(widthPx / scale.toFloat()).toInt()
        val renderer = remember(vw) { RoomRenderer(vw, virtualHeight) }
        val bitmap = remember(vw) { Bitmap.createBitmap(vw, virtualHeight, Bitmap.Config.ARGB_8888) }
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        var frame by remember { mutableLongStateOf(0L) }
        val current by rememberUpdatedState(state)
        LaunchedEffect(renderer) {
            var last = 0L
            while (true) {
                withFrameMillis { }
                val now = SystemClock.uptimeMillis()
                if (now - last < 80) continue
                last = now
                val t = LocalTime.now()
                renderer.render(current.copy(hour = t.hour + t.minute / 60f), now)
                bitmap.setPixels(renderer.buffer.px, 0, vw, 0, 0, vw, virtualHeight)
                frame = now
            }
        }
        val heightDp = with(LocalDensity.current) { (virtualHeight * scale).toDp() }
        Canvas(Modifier.fillMaxWidth().height(heightDp)) {
            frame
            drawImage(image, dstSize = IntSize(vw * scale, virtualHeight * scale), filterQuality = FilterQuality.None)
        }
    }
}

fun roomState(hero: Hero, grown: Boolean) = RoomState(
    hero.look(), hero.pet, grown, HomeCatalog.placement(hero.home), hero.wallpaper, hero.floor,
)

@Composable
fun HomeScreen(hero: Hero, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val grown by remember(hero.pet) { c.db.game().observePets().map { l -> (l.firstOrNull { it.key == hero.pet }?.growth ?: 0) >= 100 } }
        .collectAsStateWithLifecycle(initialValue = false)
    var picking by remember { mutableStateOf<Pair<DecorType, Spot?>?>(null) }
    val placement = HomeCatalog.placement(hero.home)

    Column(Modifier.fillMaxSize().background(D.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.home), onBack) {
            PixelImage(Sprites.coin, pixel = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text(goldText(hero.gold), style = DType.Number.copy(color = D.c.gold, fontSize = 18.sp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            RoomView(roomState(hero, grown))
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
                Gap(10)
                Text(stringResource(R.string.home_text), style = DType.Sub)
                listOf(DecorType.WALL, DecorType.BIG, DecorType.SMALL, DecorType.RUG).forEach { type ->
                    SectionTitle(stringResource(type.label))
                    DCard(padding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                        val spots = Spot.entries.filter { it.type == type }
                        spots.forEachIndexed { i, spot ->
                            SpotRow(spotLabel(spot), placement[spot]?.let(HomeCatalog::item)) { picking = type to spot }
                            if (i < spots.size - 1) HorizontalDivider(color = D.c.line)
                        }
                    }
                }
                SectionTitle(stringResource(R.string.decor_wallpaper) + " · " + stringResource(R.string.decor_floor))
                DCard(padding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                    SpotRow(stringResource(R.string.decor_wallpaper), HomeCatalog.item(hero.wallpaper)) { picking = DecorType.WALLPAPER to null }
                    HorizontalDivider(color = D.c.line)
                    SpotRow(stringResource(R.string.decor_floor), HomeCatalog.item(hero.floor)) { picking = DecorType.FLOOR to null }
                }
                Gap(24)
            }
        }
    }

    picking?.let { (type, spot) -> DecorPicker(hero, type, spot) { picking = null } }
}

@Composable
private fun spotLabel(spot: Spot): String {
    val side = when (spot) {
        Spot.WALL_L, Spot.BIG_L, Spot.SMALL_L -> R.string.spot_left
        Spot.WALL_R, Spot.BIG_R, Spot.SMALL_R -> R.string.spot_right
        else -> R.string.spot_center
    }
    return stringResource(spot.type.label) + " · " + stringResource(side)
}

@Composable
private fun SpotRow(label: String, item: Decor?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(D.RSm).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.Center) {
            item?.let { DecorIcon(it) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = DType.Small)
            Text(item?.let { stringResource(it.name) } ?: stringResource(R.string.spot_empty), style = DType.Strong.copy(color = if (item == null) D.c.faint else D.c.ink))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = D.c.faint)
    }
}

/** Wallpapers and floors have no sprite; show a swatch rendered by the room itself. */
@Composable
private fun DecorIcon(d: Decor, size: Int = 40) {
    when (d.type) {
        DecorType.WALLPAPER, DecorType.FLOOR -> {
            val bmp = remember(d.key) {
                val r = RoomRenderer(24, 40)
                r.render(RoomState(app.dayquest.pixel.Look(0, 0, 0, "", "tunic", "", ""), wallpaper = if (d.type == DecorType.WALLPAPER) d.key else "wp_plain", floor = if (d.type == DecorType.FLOOR) d.key else "fl_planks"), 0)
                val y0 = if (d.type == DecorType.WALLPAPER) 2 else r.floorY + 1
                Bitmap.createBitmap(r.buffer.px, y0 * 24, 24, 24, 12, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
            Canvas(Modifier.size(size.dp)) {
                drawImage(bmp, dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()), filterQuality = FilterQuality.None)
            }
        }
        else -> {
            val spr = Sprites.decor(d.key)
            val px = (size.toFloat() / maxOf(spr.w, spr.h)).coerceIn(0.6f, 3f)
            PixelImage(spr, pixel = px.dp)
        }
    }
}

@Composable
private fun DecorPicker(hero: Hero, type: DecorType, spot: Spot?, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val poor = stringResource(R.string.not_enough_gold)
    val placement = HomeCatalog.placement(hero.home)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        confirmButton = {},
        title = { Text(stringResource(type.label), style = DType.Heading) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                HomeCatalog.items.filter { it.type == type }.forEach { d ->
                    val owned = hero.ownsDecor(d.key)
                    val locked = hero.level < d.level
                    val here = when (type) {
                        DecorType.WALLPAPER -> hero.wallpaper == d.key
                        DecorType.FLOOR -> hero.floor == d.key
                        else -> spot != null && placement[spot] == d.key
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(D.RSm).background(if (here) D.c.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable(enabled = owned) { fb.tap(); c.launch { c.game.place(spot, d) }; onDismiss() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp).clip(D.RSm).background(D.c.surface), contentAlignment = Alignment.Center) { DecorIcon(d) }
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(d.name), style = DType.Strong.copy(fontSize = 15.sp, color = if (locked) D.c.faint else D.c.ink), modifier = Modifier.weight(1f))
                        when {
                            owned -> {}
                            locked -> Text(stringResource(R.string.from_level, d.level), style = DType.Small)
                            else -> PriceButton(d.price, hero.gold >= d.price, {
                                c.scope.launch {
                                    val ok = c.game.buyDecor(d)
                                    if (ok) c.game.place(spot, d)
                                    withContext(Dispatchers.Main) { if (ok) { fb.tap(); onDismiss() } else fb.show(poor) }
                                }
                            })
                        }
                    }
                }
                if (spot != null && placement[spot] != null) {
                    Gap(8)
                    SoftButton(stringResource(R.string.take_away), { c.launch { c.game.place(spot, null) }; onDismiss() }, Modifier.fillMaxWidth())
                }
            }
        },
    )
}
