package app.dayquest.ui.hero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.data.LogKind
import app.dayquest.game.Catalog
import app.dayquest.game.Slot
import app.dayquest.pixel.Look
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprites
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.look
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.pixel.SceneView
import app.dayquest.ui.shop.StatChips
import app.dayquest.ui.slotLabel
import app.dayquest.ui.tasks.StatsPanel
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.flow.map

@Composable
fun HeroScreen(hero: Hero, onSettings: () -> Unit, onHome: () -> Unit) {
    val c = LocalContext.current.container
    val pets by remember { c.db.game().observePets() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val done by remember { c.db.game().observeDoneCount() }.collectAsStateWithLifecycle(initialValue = 0)
    var slot by remember { mutableStateOf<Slot?>(null) }
    val stats = c.game.stats(hero)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            SceneView(SceneState(Catalog.scene(hero.scene).kind, 12f, hero.look(), hero.pet, (pets.firstOrNull { it.key == hero.pet }?.growth ?: 0) >= 100, null, hero.resting, app.dayquest.ui.rememberSeason()))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onSettings, modifier = Modifier.size(40.dp).clip(D.RSm).background(Color(0x55000000))) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = Color.White)
                }
            }
        }
        StatsPanel(hero)
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionTitle(stringResource(R.string.home))
            Column(Modifier.fillMaxWidth().clip(D.RMd).border(1.dp, D.c.line, D.RMd).clickable(onClick = onHome)) {
                app.dayquest.ui.home.RoomView(app.dayquest.ui.home.roomState(hero, (pets.firstOrNull { it.key == hero.pet }?.growth ?: 0) >= 100), virtualHeight = 64)
                Row(Modifier.fillMaxWidth().background(D.c.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_text), style = DType.Muted, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    SoftButton(stringResource(R.string.home_open), onHome)
                }
            }

            AchievementsSection(hero)

            SectionTitle(stringResource(R.string.appearance))
            LookEditor(hero.name, Look(hero.skin, hero.hairStyle, hero.hairColor, hero.weapon, hero.armor, hero.head, hero.shield)) { name, look ->
                c.launch { c.game.updateLook(name.ifBlank { hero.name }, look.skin, look.hairStyle, look.hairColor) }
            }

            SectionTitle(stringResource(R.string.equipment))
            DCard(padding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
                Slot.entries.forEachIndexed { i, s ->
                    val key = when (s) {
                        Slot.WEAPON -> hero.weapon
                        Slot.ARMOR -> hero.armor
                        Slot.HEAD -> hero.head
                        Slot.SHIELD -> hero.shield
                    }
                    val g = Catalog.gear(key)
                    Row(Modifier.fillMaxWidth().clip(D.RSm).clickable { slot = s }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(slotLabel(s), style = DType.Small)
                            Text(g?.let { app.dayquest.ui.gearName(it) } ?: stringResource(R.string.slot_empty), style = DType.Strong)
                        }
                        g?.let { StatChips(it.stats) }
                    }
                    if (i < Slot.entries.size - 1) HorizontalDivider(color = D.c.line)
                }
            }

            SectionTitle(stringResource(R.string.stats))
            DCard {
                listOf(
                    Triple(R.string.stat_str, stats.str, R.string.stat_str_long),
                    Triple(R.string.stat_int, stats.int, R.string.stat_int_long),
                    Triple(R.string.stat_con, stats.con, R.string.stat_con_long),
                    Triple(R.string.stat_per, stats.per, R.string.stat_per_long),
                ).forEach { (short, v, long) ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(short), style = DType.Pixel.copy(color = D.c.accent), modifier = Modifier.width(44.dp))
                        Text(v.toString(), style = DType.Number, modifier = Modifier.width(36.dp))
                        Text(stringResource(long), style = DType.Muted, modifier = Modifier.weight(1f))
                    }
                }
            }

            Gap(12)
            DCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.rest_inn), style = DType.Strong)
                        Gap(2)
                        Text(stringResource(R.string.rest_inn_text), style = DType.Muted)
                    }
                    Switch(hero.resting, { v -> c.launch { c.game.setResting(v) } }, colors = SwitchDefaults.colors(checkedTrackColor = D.c.accent))
                }
            }

            SectionTitle(stringResource(R.string.activity))
            Heatmap()
            Chronicle()
            Gap(12)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Total(stringResource(R.string.total_done), done.toString(), Modifier.weight(1f))
                Total(stringResource(R.string.bosses_beaten), hero.defeatedCount.toString(), Modifier.weight(1f))
            }
            Gap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Total(stringResource(R.string.pets_hatched), "${pets.size}", Modifier.weight(1f))
                Total(stringResource(R.string.times_fainted), hero.deaths.toString(), Modifier.weight(1f))
            }
            Gap(28)
        }
    }

    slot?.let { s -> GearPicker(hero, s) { slot = null } }
}

@Composable
private fun Total(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd).padding(14.dp)) {
        Text(value, style = DType.Number.copy(fontSize = 24.sp))
        Text(label, style = DType.Muted)
    }
}

/** Name, skin, hair style and colour, with a live preview. Shared with onboarding. */
@Composable
fun LookEditor(name: String, look: Look, showName: Boolean = true, onChange: (String, Look) -> Unit) {
    var n by remember { mutableStateOf(name) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp).clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd), contentAlignment = Alignment.Center) {
                PixelImage(Sprites.hero(look), pixel = 4.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (showName) {
                    DTextField(n, { n = it.take(24); onChange(n, look) }, placeholder = stringResource(R.string.hero_name))
                    Gap(10)
                }
                Text(stringResource(R.string.skin), style = DType.Small)
                Gap(4)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Catalog.skins.forEachIndexed { i, col -> Swatch(Color(col), look.skin == i) { onChange(n, look.copy(skin = i)) } }
                }
            }
        }
        Gap(12)
        Text(stringResource(R.string.hair), style = DType.Small)
        Gap(4)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(Catalog.HAIR_STYLES) { i ->
                val on = look.hairStyle == i
                Box(
                    Modifier.size(56.dp).clip(D.RSm).background(if (on) D.c.accentSoft else D.c.surface)
                        .border(if (on) 2.dp else 1.dp, if (on) D.c.accent else D.c.line, D.RSm).clickable { onChange(n, look.copy(hairStyle = i)) },
                    contentAlignment = Alignment.Center,
                ) { PixelImage(Sprites.heroBust(look.copy(hairStyle = i, head = "")), pixel = 2.dp) }
            }
        }
        Gap(10)
        Text(stringResource(R.string.hair_color), style = DType.Small)
        Gap(4)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Catalog.hairColors.forEachIndexed { i, col -> Swatch(Color(col), look.hairColor == i) { onChange(n, look.copy(hairColor = i)) } }
        }
    }
}

@Composable
private fun Swatch(color: Color, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(color)
            .border(if (on) 3.dp else 1.dp, if (on) D.c.accent else D.c.line, CircleShape).clickable(onClick = onClick),
    )
}

@Composable
private fun GearPicker(hero: Hero, slot: Slot, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    val owned = hero.ownedGear.split(',').mapNotNull { Catalog.gear(it) }.filter { it.slot == slot }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        confirmButton = {},
        title = { Text(slotLabel(slot), style = DType.Heading) },
        text = {
            Column {
                owned.forEach { g ->
                    val on = listOf(hero.weapon, hero.armor, hero.head, hero.shield).contains(g.key)
                    Row(
                        Modifier.fillMaxWidth().clip(D.RSm).background(if (on) D.c.accentSoft else Color.Transparent)
                            .clickable { fb.tap(); c.launch { c.game.equip(g) }; onDismiss() }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val tryOn = when (slot) {
                            Slot.WEAPON -> hero.look().copy(weapon = g.key)
                            Slot.ARMOR -> hero.look().copy(armor = g.key)
                            Slot.HEAD -> hero.look().copy(head = g.key)
                            Slot.SHIELD -> hero.look().copy(shield = g.key)
                        }
                        PixelImage(Sprites.hero(tryOn), pixel = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.dayquest.ui.gearName(g), style = DType.Strong)
                            StatChips(g.stats)
                        }
                    }
                }
                if (slot == Slot.HEAD || slot == Slot.SHIELD) {
                    Gap(8)
                    SoftButton(stringResource(R.string.unequip), { c.launch { c.game.unequip(slot) }; onDismiss() }, Modifier.fillMaxWidth())
                }
            }
        },
    )
}

/** Twelve weeks of finished tasks, GitHub-style, in chunky pixel squares. */
@Composable
private fun Heatmap() {
    val c = LocalContext.current.container
    var today by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { today = c.game.today() }
    val from = today - 83
    val counts by remember(today) {
        c.db.game().observeLogSince(from).map { l -> l.filter { it.kind == LogKind.UP || it.kind == LogKind.DAILY || it.kind == LogKind.TODO }.groupingBy { it.day }.eachCount() }
    }.collectAsStateWithLifecycle(initialValue = emptyMap())
    if (today == 0L) return
    val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val empty = D.c.line
    val accent = D.c.accent
    DCard {
        Canvas(Modifier.fillMaxWidth().aspectRatio(12f / 7f)) {
            val cell = size.width / 12f
            val gap = cell * 0.16f
            // Align columns to weeks ending today.
            val todayDow = java.time.LocalDate.ofEpochDay(today).dayOfWeek.value - 1
            for (k in 0 until 84) {
                val day = today - todayDow + (k % 7) - (11 - k / 7) * 7L
                if (day > today) continue
                val col = k / 7
                val row = k % 7
                val n = counts[day] ?: 0
                val color = if (n == 0) empty else accent.copy(alpha = 0.25f + 0.75f * (n.toFloat() / max))
                drawRoundRect(color, Offset(col * cell + gap / 2, row * cell + gap / 2), Size(cell - gap, cell - gap), CornerRadius(cell * 0.12f))
            }
        }
        Gap(6)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.less), style = DType.Small.copy(fontSize = 11.sp))
            Spacer(Modifier.width(6.dp))
            listOf(0f, 0.35f, 0.6f, 1f).forEach { a ->
                Box(Modifier.padding(horizontal = 2.dp).size(10.dp).clip(D.RXs).background(if (a == 0f) empty else accent.copy(alpha = 0.25f + 0.75f * a)))
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.more), style = DType.Small.copy(fontSize = 11.sp))
        }
    }
}

@Composable
private fun AchievementsSection(hero: Hero) {
    val c = LocalContext.current.container
    var facts by remember { mutableStateOf<app.dayquest.game.Facts?>(null) }
    LaunchedEffect(hero) { facts = c.game.facts(hero) }
    val f = facts ?: return
    val earned = hero.achievements.split(',').toSet()
    SectionTitle(stringResource(R.string.achievements)) {
        Text(
            stringResource(R.string.ach_count, app.dayquest.game.Achievements.all.count { it.key in earned }, app.dayquest.game.Achievements.all.size),
            style = DType.Number.copy(fontSize = 13.sp, color = D.c.muted),
        )
    }
    DCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        app.dayquest.game.Metric.entries.forEachIndexed { i, m ->
            val value = f.value(m)
            val next = m.targets.firstOrNull { "${m.name}:$it" !in earned }
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.width(118.dp)) {
                    m.targets.forEachIndexed { k, t ->
                        PixelImage(Sprites.medal(k, locked = "${m.name}:$t" !in earned), pixel = 1.5.dp)
                        Spacer(Modifier.width(2.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    val target = next ?: m.targets.last()
                    Text(androidx.compose.ui.res.pluralStringResource(m.title, target, target), style = DType.Body.copy(fontSize = 14.sp, color = if (next == null) D.c.good else D.c.ink))
                    if (next != null) {
                        Gap(3)
                        app.dayquest.ui.pixel.PixelBar((value.toFloat() / next).coerceIn(0f, 1f), Sprites.medalColors[m.targets.indexOf(next).coerceAtMost(4)].let { androidx.compose.ui.graphics.Color(it) }, pixel = 1.5.dp)
                        Text("${value.coerceAtMost(next)} / $next", style = DType.Small.copy(fontSize = 11.sp))
                    }
                }
            }
            if (i < app.dayquest.game.Metric.entries.size - 1) HorizontalDivider(color = D.c.line)
        }
    }
}

/** One line per year: how much got done. The longer you play, the longer it gets. */
@Composable
private fun Chronicle() {
    val c = LocalContext.current.container
    val history by remember { c.db.game().observeHistory() }.collectAsStateWithLifecycle(initialValue = emptyList())
    if (history.isEmpty()) return
    val years = history.groupBy { java.time.LocalDate.ofEpochDay(it.day).year }.toSortedMap(compareByDescending { it })
    SectionTitle(stringResource(R.string.chronicle))
    DCard {
        years.entries.forEachIndexed { i, (year, rows) ->
            val tasks = rows.filter { it.kind != LogKind.VICTORY }
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(year.toString(), style = DType.Heading, modifier = Modifier.width(72.dp))
                ChronicleStat(tasks.size, stringResource(R.string.chronicle_tasks), Modifier.weight(1f))
                ChronicleStat(tasks.map { it.day }.distinct().size, stringResource(R.string.chronicle_days), Modifier.weight(1f))
                ChronicleStat(rows.count { it.kind == LogKind.VICTORY }, stringResource(R.string.chronicle_wins), Modifier.weight(1f))
            }
            if (i < years.size - 1) HorizontalDivider(color = D.c.line)
        }
    }
}

@Composable
private fun ChronicleStat(n: Int, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(n.toString(), style = DType.Number.copy(fontSize = 17.sp))
        Text(label, style = DType.Small.copy(fontSize = 11.sp))
    }
}
