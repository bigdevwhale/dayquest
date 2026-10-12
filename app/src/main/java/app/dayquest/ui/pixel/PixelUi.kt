package app.dayquest.ui.pixel

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.pixel.Burst
import app.dayquest.pixel.SceneRenderer
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprite
import app.dayquest.pixel.Sprites
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.PixelFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.WeakHashMap
import kotlin.math.ceil
import kotlin.math.max

private val bitmaps = WeakHashMap<Sprite, ImageBitmap>()

fun Sprite.image(): ImageBitmap = synchronized(bitmaps) {
    bitmaps.getOrPut(this) { Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap() }
}

/** A sprite scaled by a whole number of device pixels, so every pixel stays a crisp square. */
@Composable
fun PixelImage(sprite: Sprite, modifier: Modifier = Modifier, pixel: Dp = 3.dp, alpha: Float = 1f, tint: Color? = null) {
    val density = LocalDensity.current
    val px = with(density) { pixel.roundToPx() }.coerceAtLeast(1)
    val img = sprite.image()
    val w = with(density) { (sprite.w * px).toDp() }
    val h = with(density) { (sprite.h * px).toDp() }
    Canvas(modifier.size(w, h)) {
        drawImage(
            img, dstSize = IntSize(sprite.w * px, sprite.h * px), filterQuality = FilterQuality.None, alpha = alpha,
            colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) },
        )
    }
}

private data class FloatText(val id: Long, val text: String, val color: Color, val x: Float, val born: Long, val lane: Int = 0)

/** A number that floats up from the hero ("+12", "−4"). */
data class Floater(val text: String, val color: Color, val atBoss: Boolean = false)

/**
 * The living scene: renders the pixel landscape at ~15 fps into a small bitmap and scales it up by an
 * integer factor. [bursts] trigger particles; [floaters] rise as pixel-font numbers.
 */
@Composable
fun SceneView(
    state: SceneState,
    modifier: Modifier = Modifier,
    virtualHeight: Int = 72,
    bursts: Flow<Burst>? = null,
    floaters: Flow<Floater>? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val scale = max(2, widthPx / 172)
        val vw = ceil(widthPx / scale.toFloat()).toInt()
        val renderer = remember(vw, virtualHeight) { SceneRenderer(vw, virtualHeight) }
        val bitmap = remember(vw, virtualHeight) { Bitmap.createBitmap(vw, virtualHeight, Bitmap.Config.ARGB_8888) }
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        var frame by remember { mutableLongStateOf(0L) }
        val current by rememberUpdatedState(state)
        val texts = remember { mutableStateListOf<FloatText>() }

        LaunchedEffect(renderer, bursts) {
            bursts?.collect { renderer.burst(it, SystemClock.uptimeMillis()) }
        }
        LaunchedEffect(renderer, floaters) {
            floaters?.collect { f ->
                val x = if (f.atBoss) renderer.w * 0.74f else renderer.heroCenter()
                            // Numbers from one action stack instead of overlapping.
                val lane = texts.count { SystemClock.uptimeMillis() - it.born < 300 }
                texts += FloatText(SystemClock.uptimeMillis() + texts.size, f.text, f.color, x, SystemClock.uptimeMillis() + lane * 90L, lane)
                launch {
                    delay(1800)
                    texts.removeAll { SystemClock.uptimeMillis() - it.born >= 1500 }
                }
            }
        }
        LaunchedEffect(renderer) {
            var last = 0L
            while (true) {
                withFrameMillis { }
                val now = SystemClock.uptimeMillis()
                if (now - last < 66) continue
                last = now
                val t = LocalTime.now()
                renderer.render(current.copy(hour = t.hour + t.minute / 60f), now)
                bitmap.setPixels(renderer.buffer.px, 0, vw, 0, 0, vw, virtualHeight)
                frame = now
            }
        }
        val density = LocalDensity.current
        val heightDp = with(density) { (virtualHeight * scale).toDp() }
        Box(Modifier.fillMaxWidth().height(heightDp)) {
            Canvas(Modifier.fillMaxWidth().height(heightDp)) {
                frame // redraw on every rendered frame
                drawImage(image, dstSize = IntSize(vw * scale, virtualHeight * scale), filterQuality = FilterQuality.None)
            }
            texts.forEach { t ->
                if (frame < t.born) return@forEach
                val age = ((frame - t.born) / 1200f).coerceIn(0f, 1f)
                val y = (renderer.groundY - 30 - t.lane * 8 - age * 14) * scale
                Text(
                    t.text,
                    style = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = t.color, textAlign = TextAlign.Center,
                        shadow = androidx.compose.ui.graphics.Shadow(Color(0xAA000000), Offset(0f, 3f), 0f)),
                    modifier = Modifier
                        .offset { IntOffset((t.x * scale).toInt() - 150, y.toInt()) }
                        .size(with(density) { 300.toDp() }, with(density) { 90.toDp() })
                        .alpha(1f - age * age),
                )
            }
        }
    }
}

/**
 * A chunky pixel bar with bevel highlights, a damage "ghost" that drains after the real value,
 * and stepped corners instead of round ones.
 */
@Composable
fun PixelBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = D.c.line, pixel: Dp = 2.dp, cells: Int = 6) {
    val target = fraction.coerceIn(0f, 1f)
    val shown = remember { Animatable(target) }
    val ghost = remember { Animatable(target) }
    LaunchedEffect(target) {
        launch { shown.animateTo(target, tween(380)) }
        if (target < ghost.value) {
            delay(420)
            ghost.animateTo(target, tween(520, easing = LinearEasing))
        } else ghost.snapTo(target)
    }
    val outline = if (D.c.dark) Color(0xFF0B0915) else Color(0xFF2A2240)
    Canvas(modifier.fillMaxWidth().height(pixel * cells)) {
        val p = pixel.toPx().let { ceil(it) }
        val w = size.width
        val h = size.height
        // Body with notched corners
        drawRect(outline, Offset(p, 0f), Size(w - 2 * p, h))
        drawRect(outline, Offset(0f, p), Size(w, h - 2 * p))
        val ix = p
        val iy = p
        val iw = w - 2 * p
        val ih = h - 2 * p
        drawRect(track, Offset(ix, iy), Size(iw, ih))
        drawRect(Color.Black.copy(alpha = 0.18f), Offset(ix, iy), Size(iw, p))
        val gw = (iw * ghost.value / p).toInt() * p
        if (gw > 0) drawRect(Color.White.copy(alpha = 0.75f), Offset(ix, iy), Size(gw, ih))
        val fw = (iw * shown.value / p).toInt() * p
        if (fw > 0) {
            drawRect(color, Offset(ix, iy), Size(fw, ih))
            drawRect(lerp(color, Color.White, 0.45f), Offset(ix, iy), Size(fw, p))
            drawRect(lerp(color, Color.Black, 0.28f), Offset(ix, iy + ih - p), Size(fw, p))
            if (fw > 4 * p) drawRect(Color.White.copy(alpha = 0.7f), Offset(ix + p, iy + p), Size(p * 2, p))
        }
    }
}

/** A tiny pixel glyph (check, plus, minus) tinted to any colour. */
@Composable
fun Glyph(sprite: Sprite, color: Color, pixel: Dp = 2.dp) = PixelImage(sprite, pixel = pixel, tint = color)

val glyphCheck get() = Sprites.check
val glyphPlus get() = Sprites.plus
val glyphMinus get() = Sprites.minus
