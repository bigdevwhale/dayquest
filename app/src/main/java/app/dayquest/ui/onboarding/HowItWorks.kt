package app.dayquest.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.game.Catalog
import app.dayquest.game.Potion
import app.dayquest.game.SceneKind
import app.dayquest.game.Species
import app.dayquest.pixel.Look
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprites
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.pixel.Glyph
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.pixel.SceneView
import app.dayquest.ui.pixel.glyphMinus
import app.dayquest.ui.pixel.glyphPlus
import app.dayquest.ui.rememberSeason
import app.dayquest.ui.tasks.CheckBoxPixel
import app.dayquest.ui.theme.Bands
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.launch

private const val PAGES = 5

/**
 * "How it works": five short pages that explain the game with the game's own art.
 * Shown before the hero is created, and again from Settings → How to play.
 */
@Composable
fun HowItWorks(look: Look, finishLabel: String, onFinish: () -> Unit, onSkip: (() -> Unit)? = null) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val season = rememberSeason()
    Column(Modifier.fillMaxSize().background(D.c.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (onSkip != null && pager.currentPage < PAGES - 1) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.guide_skip), color = D.c.muted, style = DType.Button) }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().heightIn(min = 250.dp), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> HeroArt(look, season)
                        1 -> TasksArt()
                        2 -> LootArt()
                        3 -> BossArt(look, season)
                        else -> KindArt()
                    }
                }
                Gap(24)
                Text(stringResource(listOf(R.string.g1_title, R.string.g2_title, R.string.g3_title, R.string.g4_title, R.string.g5_title)[page]), style = DType.Title, textAlign = TextAlign.Center)
                Gap(12)
                if (page == 1) {
                    Text(stringResource(R.string.g2_habit) + "\n" + stringResource(R.string.g2_daily) + "\n" + stringResource(R.string.g2_todo), style = DType.Sub, textAlign = TextAlign.Center)
                } else {
                    Text(stringResource(listOf(R.string.g1_text, 0, R.string.g3_text, R.string.g4_text, R.string.g5_text)[page]), style = DType.Sub, textAlign = TextAlign.Center)
                }
                Gap(16)
            }
        }
        // Page dots
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(PAGES) { i ->
                val on = i == pager.currentPage
                val w by animateDpAsState(if (on) 22.dp else 8.dp, label = "dot")
                val c by animateColorAsState(if (on) D.c.accent else D.c.line, label = "dotc")
                Box(Modifier.padding(horizontal = 4.dp).size(width = w, height = 8.dp).clip(D.RXs).background(c))
            }
        }
        val last = pager.currentPage == PAGES - 1
        PrimaryButton(
            if (last) finishLabel else stringResource(R.string.next),
            { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Frame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(D.RLg).background(D.c.surface).border(1.dp, D.c.line, D.RLg)) { content() }
}

@Composable
private fun HeroArt(look: Look, season: app.dayquest.game.Season) {
    Frame {
        Column {
            SceneView(SceneState(SceneKind.MEADOW, 12f, look, "fox:base", season = season), virtualHeight = 60)
            Column(Modifier.padding(14.dp)) {
                ArtBar(Sprites.heart, 0.8f, D.c.hp)
                Gap(6)
                ArtBar(Sprites.star, 0.45f, D.c.xp)
            }
        }
    }
}

@Composable
private fun ArtBar(icon: app.dayquest.pixel.Sprite, f: Float, c: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { PixelImage(icon, pixel = 2.dp) }
        Spacer(Modifier.width(8.dp))
        PixelBar(f, c, Modifier.weight(1f))
    }
}

@Composable
private fun TasksArt() {
    Frame {
        Column(Modifier.padding(12.dp)) {
            MockRow(Bands[4], { Glyph(glyphPlus, Color.White, 3.dp) }, stringResource(R.string.st_water), trailing = Bands[2])
            Gap(8)
            MockRow(Bands[3], { CheckBoxPixel(false, Bands[4], 42) }, stringResource(R.string.st_read), box = false)
            Gap(8)
            MockRow(Bands[3], { CheckBoxPixel(true, D.c.faint, 42) }, stringResource(R.string.st_todo_desk), box = false, done = true)
        }
    }
}

@Composable
private fun MockRow(color: Color, lead: @Composable () -> Unit, title: String, box: Boolean = true, trailing: Color? = null, done: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(D.RMd).background(D.c.bg).border(1.dp, D.c.line, D.RMd).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (box) Box(Modifier.size(42.dp).clip(D.RSm).background(color), contentAlignment = Alignment.Center) { lead() } else lead()
        Spacer(Modifier.width(12.dp))
        Text(title, style = DType.Strong.copy(fontSize = 15.sp, color = if (done) D.c.muted else D.c.ink), modifier = Modifier.weight(1f))
        if (trailing != null) Box(Modifier.size(42.dp).clip(D.RSm).background(trailing), contentAlignment = Alignment.Center) { Glyph(glyphMinus, Color.White, 3.dp) }
    }
}

@Composable
private fun LootArt() {
    Frame {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelImage(Sprites.star, pixel = 5.dp)
                PixelImage(Sprites.coin, pixel = 5.dp)
                PixelImage(Sprites.food(app.dayquest.game.Food.BERRY), pixel = 4.dp)
            }
            Gap(20)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelImage(Sprites.egg(Species.FOX), pixel = 4.dp)
                Text("  +  ", style = DType.Title.copy(color = D.c.faint))
                PixelImage(Sprites.potion(Potion.GOLDEN), pixel = 4.dp)
                Text("  =  ", style = DType.Title.copy(color = D.c.faint))
                PixelImage(Sprites.pet(Species.FOX, Potion.GOLDEN), pixel = 4.dp)
            }
        }
    }
}

@Composable
private fun BossArt(look: Look, season: app.dayquest.game.Season) {
    Frame {
        Column {
            SceneView(SceneState(SceneKind.MEADOW, 12f, look, "fox:base", boss = Catalog.bosses.first(), season = season), virtualHeight = 64)
            Column(Modifier.padding(14.dp)) {
                ArtBar(Sprites.heart, 0.6f, D.c.hp)
                Gap(6)
                ArtBar(Sprites.navIcon(3), 0.35f, Color(0xFFB45CFF))
            }
        }
    }
}

@Composable
private fun KindArt() {
    Frame {
        Row(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            PixelImage(Sprites.decor("bed"), pixel = 3.dp)
            Box(Modifier.size(64.dp).clip(D.RLg).background(D.c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.Undo, null, Modifier.size(34.dp), tint = D.c.accent)
            }
            PixelImage(Sprites.heart, pixel = 5.dp)
        }
    }
}
