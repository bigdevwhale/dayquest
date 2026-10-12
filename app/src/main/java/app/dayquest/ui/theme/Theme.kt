package app.dayquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.data.ThemeMode

/** Cozy dusk violets with parchment for light mode: a frame that lets the pixel art glow. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val hp: Color,
    val hpSoft: Color,
    val xp: Color,
    val xpSoft: Color,
    val gold: Color,
    val good: Color,
    val bad: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFF6F2EA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFFBF8F2),
    line = Color(0xFFE9E2D5),
    ink = Color(0xFF231E33),
    muted = Color(0xFF6B6480),
    faint = Color(0xFFA39DB5),
    accent = Color(0xFF6B4EE6),
    accentSoft = Color(0xFFECE7FF),
    onAccent = Color(0xFFFFFFFF),
    hp = Color(0xFFE5484D),
    hpSoft = Color(0xFFFBE0E1),
    xp = Color(0xFFE9A92A),
    xpSoft = Color(0xFFFBEFD3),
    gold = Color(0xFFC98A12),
    good = Color(0xFF2FA866),
    bad = Color(0xFFE5484D),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF13111F),
    surface = Color(0xFF1C1930),
    surface2 = Color(0xFF221E3A),
    line = Color(0xFF2E2950),
    ink = Color(0xFFEDEAF7),
    muted = Color(0xFFA9A3C4),
    faint = Color(0xFF6F6890),
    accent = Color(0xFF9B82FF),
    accentSoft = Color(0xFF2B2452),
    onAccent = Color(0xFF130F26),
    hp = Color(0xFFFF5C64),
    hpSoft = Color(0xFF3B1C28),
    xp = Color(0xFFFFC94A),
    xpSoft = Color(0xFF3A2F1A),
    gold = Color(0xFFF2C14E),
    good = Color(0xFF4FD08A),
    bad = Color(0xFFFF5C64),
    dark = true,
)

/** Habitica-style task colours from neglected to mastered, indexed by [app.dayquest.game.Rules.band]. */
val Bands = listOf(
    Color(0xFFC8364A), Color(0xFFE5574F), Color(0xFFF08A4B), Color(0xFFF2C14E),
    Color(0xFF4FC07A), Color(0xFF3FB7C9), Color(0xFF4F8BE8),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val PixelFont = FontFamily(
    Font(R.font.pixel_medium, FontWeight.Medium),
    Font(R.font.pixel_bold, FontWeight.Bold),
)

object D {
    val c: Palette @Composable get() = LocalPalette.current

    val RLg = RoundedCornerShape(22.dp)
    val RMd = RoundedCornerShape(16.dp)
    val RSm = RoundedCornerShape(11.dp)
    val RXs = RoundedCornerShape(7.dp)
}

object DType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, color = D.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp, color = D.c.ink)
    val Heading: TextStyle @Composable get() = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 25.sp, color = D.c.ink)
    val Pixel: TextStyle @Composable get() = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = D.c.ink)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.08.em, color = D.c.faint)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = D.c.muted)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em, color = D.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 21.sp, color = D.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp, color = D.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = D.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, fontFeatureSettings = "tnum", color = D.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DayquestTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                surfaceContainerLowest = p.bg,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.hp,
                inverseSurface = p.ink,
                inverseOnSurface = p.bg,
            ),
            typography = MaterialTheme.typography.copy(
                // No baked-in colour: Material components (snackbars, dialogs) pick their own content colour.
                bodyLarge = DType.Body.copy(color = Color.Unspecified),
                bodyMedium = DType.Body.copy(fontSize = 14.sp, color = Color.Unspecified),
                labelLarge = DType.Button,
            ),
            content = content,
        )
    }
}
