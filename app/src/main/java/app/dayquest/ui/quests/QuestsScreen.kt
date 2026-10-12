package app.dayquest.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.game.Boss
import app.dayquest.game.BossKind
import app.dayquest.game.Catalog
import app.dayquest.game.Difficulty
import app.dayquest.game.Offers
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.bossName
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.gainText
import app.dayquest.ui.goldText
import app.dayquest.ui.itemName
import app.dayquest.ui.look
import app.dayquest.ui.num
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.pixel.SceneView
import app.dayquest.ui.rememberSeason
import app.dayquest.ui.tasks.rememberSceneFx
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.flow.map

private val BossBar = Color(0xFFB45CFF)

@Composable
fun QuestsScreen(hero: Hero) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val active = Catalog.boss(hero.quest)
    var retreat by remember { mutableStateOf(false) }
    val busy = stringResource(R.string.quest_busy)
    val fx = rememberSceneFx()
    val season = rememberSeason()
    var offers by remember { mutableStateOf<Offers?>(null) }
    LaunchedEffect(hero.questsDone, hero.level, hero.quest) { offers = c.game.offers(hero) }
    val grown by remember(hero.pet) { c.db.game().observePets().map { l -> (l.firstOrNull { it.key == hero.pet }?.growth ?: 0) >= 100 } }
        .collectAsStateWithLifecycle(initialValue = false)
    val start: (Boss) -> Unit = { b -> if (active != null) fb.show(busy) else c.launch { c.game.startQuest(b) } }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.nav_quests), style = DType.Title, modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
            app.dayquest.ui.components.Tip("quests", stringResource(R.string.tip_quests), Modifier.padding(bottom = 12.dp))
        }
        item {
            if (active != null) {
                Column(Modifier.fillMaxWidth().clip(D.RLg).background(D.c.surface).border(1.dp, D.c.line, D.RLg)) {
                    SceneView(
                        SceneState(Catalog.scene(hero.scene).kind, 12f, hero.look(), hero.pet, grown, active, hero.resting, season),
                        bursts = fx.bursts, floaters = fx.floaters,
                    )
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.quest_active).uppercase(), style = DType.Eyebrow)
                        Gap(4)
                        Text(bossName(active), style = DType.Heading)
                        Gap(10)
                        PixelBar((hero.questHp / active.hp).toFloat(), BossBar)
                        Gap(4)
                        Text("${num(hero.questHp)} / ${num(active.hp)}", style = DType.Number.copy(color = D.c.muted, fontSize = 13.sp))
                        Gap(10)
                        Text(stringResource(R.string.quest_hint), style = DType.Sub)
                        Gap(4)
                        Text(stringResource(R.string.quest_strength, gainText(active.str * Difficulty.EASY.mult * 1.5)), style = DType.Muted)
                        Gap(12)
                        SoftButton(stringResource(R.string.quest_abandon), { retreat = true }, color = D.c.hp, soft = D.c.hpSoft)
                    }
                }
            } else {
                Text(stringResource(R.string.quest_none), style = DType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        val o = offers
        if (o != null) {
            o.weekly?.takeIf { it.record != active?.record }?.let { b ->
                item(key = "weekly") {
                    SectionTitle(stringResource(R.string.week_boss))
                    BossCard(b, hero, active != null, stringResource(R.string.once_week), onStart = { start(b) })
                }
            }
            o.seasonal?.takeIf { it.record != active?.record }?.let { b ->
                item(key = "seasonal") {
                    SectionTitle(stringResource(R.string.season_boss, stringResource(o.season.label)))
                    BossCard(b, hero, active != null, stringResource(R.string.once_season), onStart = { start(b) })
                }
            }
            item(key = "main") { SectionTitle(stringResource(R.string.main_bosses)) }
            items(o.main, key = { it.record }) { b -> BossCard(b, hero, active != null, null, onStart = { start(b) }) }
        }
    }

    if (retreat) ConfirmDialog(
        stringResource(R.string.quest_abandon_q), stringResource(R.string.quest_abandon_text), stringResource(R.string.quest_abandon),
        onConfirm = { c.launch { c.game.abandonQuest() } }, onDismiss = { retreat = false }, danger = true,
    )
}

@Composable
private fun BossCard(b: Boss, hero: Hero, busy: Boolean, tag: String?, onStart: () -> Unit) {
    val locked = hero.level < b.level
    val wins = hero.wins(b.record)
    val current = hero.quest.isNotEmpty() && Catalog.recordKey(hero.quest) == b.record
    Column(
        Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clip(D.RMd)
            .background(if (current) D.c.accentSoft else D.c.surface)
            .border(1.dp, if (current) D.c.accent.copy(alpha = 0.5f) else D.c.line, D.RMd)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).clip(D.RSm).background(D.c.surface2), contentAlignment = Alignment.Center) {
                PixelImage(Sprites.boss(b.shape, b.primary, b.secondary), pixel = 2.5.dp, alpha = if (locked) 0.25f else 1f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(bossName(b), style = DType.Heading.copy(fontSize = 21.sp, color = if (locked) D.c.faint else D.c.ink))
                Gap(2)
                Row {
                    Text("${num(b.hp)} HP", style = DType.Number.copy(fontSize = 13.sp, color = BossBar))
                    if (tag != null) Text("  ·  $tag", style = DType.Small)
                }
                Gap(4)
                Text(stringResource(b.story), style = DType.Muted)
            }
        }
        Gap(10)
        val loot = (listOf("${goldText(b.gold)} ${stringResource(R.string.gold).lowercase()}", "${num(b.xp)} XP") +
            b.rewards.map { (k, n) -> "${itemName(k)} ×$n" }).joinToString(" · ")
        Text(stringResource(R.string.quest_rewards, loot), style = DType.Small)
        Gap(10)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (wins > 0 && b.kind == BossKind.MAIN) {
                Text(stringResource(R.string.wins, wins), style = DType.Pixel.copy(color = D.c.good), modifier = Modifier.weight(1f))
            } else Spacer(Modifier.weight(1f))
            when {
                current -> {}
                locked -> Text(stringResource(R.string.quest_locked, b.level), style = DType.Small)
                busy -> {}
                else -> PrimaryButton(stringResource(if (wins > 0) R.string.quest_again else R.string.quest_start), onStart, Modifier.width(150.dp))
            }
        }
    }
}
