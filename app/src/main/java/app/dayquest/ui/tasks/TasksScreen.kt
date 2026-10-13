package app.dayquest.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.CheckItem
import app.dayquest.data.Hero
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Catalog
import app.dayquest.game.DayEvent
import app.dayquest.game.Outcome
import app.dayquest.game.Rules
import app.dayquest.pixel.Burst
import app.dayquest.pixel.SceneState
import app.dayquest.pixel.Sprites
import app.dayquest.ui.Feedback
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.MomentDialog
import app.dayquest.ui.Sfx
import app.dayquest.ui.Sound
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.Segmented
import app.dayquest.ui.components.Tip
import app.dayquest.ui.dayLabel
import app.dayquest.ui.events.EventBanner
import app.dayquest.ui.events.eventSprite
import app.dayquest.ui.gainText
import app.dayquest.ui.goldText
import app.dayquest.ui.itemLabel
import app.dayquest.ui.look
import app.dayquest.ui.num
import app.dayquest.ui.pixel.Floater
import app.dayquest.ui.pixel.Glyph
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.pixel.SceneView
import app.dayquest.ui.pixel.glyphCheck
import app.dayquest.ui.pixel.glyphMinus
import app.dayquest.ui.pixel.glyphPlus
import app.dayquest.ui.theme.Bands
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import app.dayquest.ui.timeLabel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

private enum class Board { HABITS, DAILIES, TODOS }

/** Routes every game outcome to the scene: particles plus floating numbers. */
class SceneFx {
    val bursts = MutableSharedFlow<Burst>(extraBufferCapacity = 32)
    val floaters = MutableSharedFlow<Floater>(extraBufferCapacity = 32)

    fun play(o: Outcome, xpC: Color, goldC: Color, hpC: Color, healC: Color) {
        if (o.xp > 0) { bursts.tryEmit(Burst.XP); floaters.tryEmit(Floater("+${gainText(o.xp)} XP", xpC)) }
        if (o.gold > 0) { bursts.tryEmit(Burst.GOLD); floaters.tryEmit(Floater("+${goldText(o.gold)}", goldC)) }
        if (o.hp < 0) { bursts.tryEmit(Burst.HURT); floaters.tryEmit(Floater("−${gainText(-o.hp)}", hpC)) }
        if (o.hp > 0) { bursts.tryEmit(Burst.HEAL); floaters.tryEmit(Floater("+${gainText(o.hp)}", healC)) }
        if (o.boss > 0) { bursts.tryEmit(if (o.crit) Burst.CRIT else Burst.HIT); floaters.tryEmit(Floater((if (o.crit) "!" else "") + gainText(o.boss), Color.White, atBoss = true)) }
        if (o.drop.isNotEmpty()) bursts.tryEmit(Burst.DROP)
        if (o.levelUp > 0) bursts.tryEmit(Burst.LEVEL)
        if (o.bossDefeated != null) bursts.tryEmit(Burst.VICTORY)
        if (o.perfect) bursts.tryEmit(Burst.FIREWORKS)
        if (o.jackpot) floaters.tryEmit(Floater("JACKPOT ×5", goldC))
        if (o.bounty) { bursts.tryEmit(Burst.GOLD); floaters.tryEmit(Floater("+${goldText(o.bountyGold)}", goldC)) }
    }
}

@Composable
fun rememberSceneFx(): SceneFx {
    val c = LocalContext.current.container
    val fx = remember { SceneFx() }
    val xp = D.c.xp
    val gold = D.c.gold
    val hp = D.c.hp
    val heal = D.c.good
    LaunchedEffect(Unit) { c.game.fx.collect { fx.play(it, xp, gold, hp, heal) } }
    return fx
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(hero: Hero, onChest: () -> Unit, onEdit: (Long) -> Unit, onNew: (TaskType, String) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val fb = LocalFeedback.current
    val tasks by remember { c.db.game().observeTasks() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val checklist by remember { c.db.game().observeChecklist().map { l -> l.groupBy { it.taskId } } }.collectAsStateWithLifecycle(initialValue = emptyMap())
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val today = Rules.logicalDay(LocalDateTime.now(), settings?.dayStart ?: 0)
    val doneToday by remember(today) { c.db.game().observeDoneOn(today) }.collectAsStateWithLifecycle(initialValue = 0)
    var cardOpen by remember { mutableStateOf(false) }
    var board by rememberSaveable { mutableStateOf(Board.DAILIES) }
    val expanded = remember { mutableStateListOf<Long>() }
    var showDone by rememberSaveable { mutableStateOf(false) }
    var showNotDue by rememberSaveable { mutableStateOf(false) }
    val fx = rememberSceneFx()

    val habits = tasks.filter { it.type == TaskType.HABIT }
    val dailies = tasks.filter { it.type == TaskType.DAILY }
    val dueDailies = dailies.filter { Rules.isDue(it, today) }
    val todos = tasks.filter { it.type == TaskType.TODO }

    val actions = remember(fb) { Actions(c, fb, context) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { HeroHeader(hero, fx, onSettings, onChest) }
        stickyHeader(key = "tabs") {
            Column(Modifier.fillMaxWidth().background(D.c.bg).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Segmented(
                    options = Board.entries.toList(), selected = board, onSelect = { board = it },
                    label = {
                        when (it) {
                            Board.HABITS -> stringResource(R.string.tab_habits)
                            Board.DAILIES -> stringResource(R.string.tab_dailies) + "  " + dueDailies.count { d -> d.doneDay == today } + "/" + dueDailies.size
                            Board.TODOS -> stringResource(R.string.tab_todos) + todos.count { t -> !t.done }.let { n -> if (n > 0) "  $n" else "" }
                        }
                    },
                )
            }
        }
        item(key = "tip") { Tip("tasks", stringResource(R.string.tip_tasks), Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        if (hero.eventDay == today) item(key = "event") {
            val ev = DayEvent.of(today)
            EventBanner(ev, doneToday, hero.bountyDay == today, { cardOpen = true }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            if (cardOpen) MomentDialog(eventSprite(ev.icon), stringResource(ev.title), stringResource(ev.text), stringResource(R.string.ok)) { cardOpen = false }
        }
        item(key = "quick-$board") {
            QuickAdd(
                hint = stringResource(
                    when (board) {
                        Board.HABITS -> R.string.add_habit_hint
                        Board.DAILIES -> R.string.add_daily_hint
                        Board.TODOS -> R.string.add_todo_hint
                    }
                ),
                onAdd = { title -> c.launch { quickAdd(c, board.type, title, today) } },
                onMore = { onNew(board.type, it) },
            )
        }
        when (board) {
            Board.HABITS -> {
                if (habits.isEmpty()) item { Empty(stringResource(R.string.empty_habits)) }
                items(habits, key = { it.id }) { t ->
                    HabitRow(t, onClick = { onEdit(t.id) }, onUp = { actions.habit(t, true) }, onDown = { actions.habit(t, false) })
                }
            }
            Board.DAILIES -> {
                if (dailies.isEmpty()) item { Empty(stringResource(R.string.empty_dailies)) }
                val open = dueDailies.filter { it.doneDay != today }
                val done = dueDailies.filter { it.doneDay == today }
                if (dueDailies.isNotEmpty()) item(key = "progress") {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.dailies_progress, done.size, dueDailies.size), style = DType.Small)
                    }
                }
                taskItems(open + done, checklist, expanded, today, actions, onEdit)
                val notDue = dailies - dueDailies.toSet()
                if (notDue.isNotEmpty()) {
                    item(key = "notdue") { Fold(stringResource(R.string.not_due), notDue.size, showNotDue) { showNotDue = !showNotDue } }
                    if (showNotDue) taskItems(notDue, checklist, expanded, today, actions, onEdit, dim = true)
                }
            }
            Board.TODOS -> {
                val open = todos.filter { !it.done }.sortedWith(compareBy({ if (it.dueDay >= 0) it.dueDay else Long.MAX_VALUE }, { it.position }))
                val done = todos.filter { it.done }.sortedByDescending { it.doneAt }
                if (todos.isEmpty()) item { Empty(stringResource(R.string.empty_todos)) }
                taskItems(open, checklist, expanded, today, actions, onEdit)
                if (done.isNotEmpty()) {
                    item(key = "done") { Fold(stringResource(R.string.completed), done.size, showDone) { showDone = !showDone } }
                    if (showDone) taskItems(done.take(50), checklist, expanded, today, actions, onEdit, dim = true)
                }
            }
        }
    }
}

private val Board.type get() = when (this) {
    Board.HABITS -> TaskType.HABIT
    Board.DAILIES -> TaskType.DAILY
    Board.TODOS -> TaskType.TODO
}

suspend fun quickAdd(c: app.dayquest.AppContainer, type: TaskType, title: String, today: Long) {
    val db = c.db.game()
    db.insert(Task(type = type, title = title.trim(), position = db.nextPosition(type), startDay = today, up = true, down = type == TaskType.HABIT))
}

/** Scoring with feedback: haptics, a snackbar with undo, and the scene's fireworks. */
class Actions(private val c: app.dayquest.AppContainer, private val fb: Feedback, private val context: android.content.Context) {
    private fun s(id: Int, vararg a: Any) = context.getString(id, *a)

    private fun report(o: Outcome?) {
        o ?: return
        val parts = buildList {
            if (o.xp > 0 || o.gold > 0) add(s(R.string.snack_gain, gainText(o.xp), goldText(o.gold)))
            if (o.hp < 0) add(s(R.string.snack_hurt, gainText(-o.hp)))
            if (o.boss > 0) add(s(if (o.crit) R.string.snack_crit else R.string.snack_hit, gainText(o.boss)))
            if (o.drop.isNotEmpty()) add(s(R.string.snack_drop, context.itemLabel(o.drop)))
        }
        if (parts.isNotEmpty()) fb.show(parts.joinToString("\n"), s(R.string.undo), o.logId)
    }

    fun habit(t: Task, up: Boolean) {
        if (up) fb.tap() else fb.thud()
        c.launch { report(c.game.scoreHabit(t.id, up)) }
    }

    fun daily(t: Task, done: Boolean) {
        fb.tap()
        if (!done) Sfx.play(Sound.UNDO)
        c.launch { report(c.game.setDaily(t.id, done)) }
    }

    fun todo(t: Task, done: Boolean) {
        fb.tap()
        if (!done) Sfx.play(Sound.UNDO)
        c.launch { report(c.game.setTodo(t.id, done)) }
    }

    fun check(item: CheckItem) {
        fb.tap()
        c.launch { c.db.game().updateCheck(item.copy(done = !item.done)) }
    }
}

private fun LazyListScope.taskItems(
    list: List<Task>, checklist: Map<Long, List<CheckItem>>, expanded: MutableList<Long>, today: Long,
    actions: Actions, onEdit: (Long) -> Unit, dim: Boolean = false,
) {
    items(list, key = { it.id }) { t ->
        val items = checklist[t.id].orEmpty()
        TaskRow(
            t, items, today, expanded = t.id in expanded, dim = dim,
            onToggle = { done -> if (t.type == TaskType.DAILY) actions.daily(t, done) else actions.todo(t, done) },
            onExpand = { if (t.id in expanded) expanded.remove(t.id) else expanded.add(t.id) },
            onCheck = actions::check,
            onClick = { onEdit(t.id) },
        )
    }
}

// ---- Header ------------------------------------------------------------------------------

@Composable
private fun HeroHeader(hero: Hero, fx: SceneFx, onSettings: () -> Unit, onChest: () -> Unit) {
    val boss = Catalog.boss(hero.quest)
    val db = LocalContext.current.container.db.game()
    val pet by remember(hero.pet) { db.observePets().map { l -> l.firstOrNull { it.key == hero.pet } } }
        .collectAsStateWithLifecycle(initialValue = null)
    Column(Modifier.fillMaxWidth()) {
        Box {
            SceneView(
                SceneState(Catalog.scene(hero.scene).kind, 12f, hero.look(), hero.pet, (pet?.growth ?: 0) >= 100, boss, hero.resting, app.dayquest.ui.rememberSeason()),
                bursts = fx.bursts, floaters = fx.floaters,
            )
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Unopened chests wait here until tapped.
                if (hero.chests > 0) {
                    Row(
                        Modifier.height(40.dp).clip(D.RSm).background(Color(0x55000000)).clickable(onClick = onChest).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PixelImage(Sprites.chest, pixel = 1.75.dp)
                        if (hero.chests > 1) {
                            Spacer(Modifier.width(6.dp))
                            Text("×${hero.chests}", style = DType.Number.copy(color = Color.White, fontSize = 14.sp))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings, modifier = Modifier.size(40.dp).clip(D.RSm).background(Color(0x55000000))) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = Color.White)
                }
            }
        }
        StatsPanel(hero)
    }
}

@Composable
fun StatsPanel(hero: Hero, modifier: Modifier = Modifier) {
    val next = Rules.xpToNext(hero.level)
    val boss = Catalog.boss(hero.quest)
    Column(
        modifier
            .fillMaxWidth()
            .background(D.c.surface)
            .border(1.dp, D.c.line)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(D.RXs).background(D.c.accent).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(stringResource(R.string.lv, hero.level), style = DType.Pixel.copy(color = D.c.onAccent, fontSize = 17.sp))
            }
            Spacer(Modifier.width(10.dp))
            Text(hero.name, style = DType.Heading.copy(fontSize = 21.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            PixelImage(Sprites.coin, pixel = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text(goldText(hero.gold), style = DType.Number.copy(color = D.c.gold, fontSize = 17.sp))
        }
        Gap(10)
        StatLine(Sprites.heart, hero.hp / Rules.MAX_HP, D.c.hp, "${num(hero.hp.coerceAtLeast(0.0))} / ${Rules.MAX_HP.toInt()}")
        Gap(6)
        StatLine(Sprites.star, (hero.xp / next).toFloat(), D.c.xp, "${num(hero.xp)} / $next")
        if (boss != null) {
            Gap(6)
            StatLine(Sprites.boss(boss.shape, boss.primary, boss.secondary), (hero.questHp / boss.hp).toFloat(), Color(0xFFB45CFF), "${num(hero.questHp)} / ${num(boss.hp)}", iconPixel = 0.5f)
        }
    }
}

@Composable
private fun StatLine(icon: app.dayquest.pixel.Sprite, fraction: Number, color: Color, label: String, iconPixel: Float = 2f) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { PixelImage(icon, pixel = iconPixel.dp) }
        Spacer(Modifier.width(8.dp))
        PixelBar(fraction.toFloat(), color, Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(label, style = DType.Number.copy(fontSize = 13.sp, color = D.c.muted), modifier = Modifier.width(72.dp), maxLines = 1)
    }
}

// ---- Rows --------------------------------------------------------------------------------

@Composable
private fun QuickAdd(hint: String, onAdd: (String) -> Unit, onMore: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        DTextField(
            text, { text = it }, Modifier.weight(1f), placeholder = hint,
            onDone = { if (text.isNotBlank()) { onAdd(text); text = "" } },
            trailing = {
                Row {
                    IconButton(onClick = { onMore(text); text = "" }) { Icon(Icons.Rounded.Tune, null, tint = D.c.muted) }
                    IconButton(onClick = { if (text.isNotBlank()) { onAdd(text); text = "" } else onMore("") }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = D.c.accent)
                    }
                }
            },
        )
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, style = DType.Sub, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp))
}

@Composable
private fun Fold(title: String, count: Int, open: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${title.uppercase()} · $count", style = DType.Eyebrow, modifier = Modifier.weight(1f))
        Icon(if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = D.c.faint)
    }
}

/** A button that squishes when pressed, like a game key. */
@Composable
private fun PressBox(color: Color, enabled: Boolean, onClick: () -> Unit, size: Int = 46, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.88f else 1f, label = "press")
    Box(
        Modifier
            .size(size.dp)
            .scale(s)
            .clip(D.RSm)
            .background(if (enabled) color else D.c.line.copy(alpha = 0.6f))
            .clickable(enabled = enabled, interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun HabitRow(t: Task, onClick: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    val band = Bands[Rules.band(t.value)]
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(D.RMd)
            .background(D.c.surface)
            .border(1.dp, D.c.line, D.RMd)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressBox(band, t.up, onUp) { Glyph(glyphPlus, Color.White, 3.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = DType.Strong.copy(fontSize = 15.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (t.notes.isNotBlank()) Text(t.notes, style = DType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.countUp + t.countDown > 0) {
                Gap(2)
                Text(
                    listOfNotNull(if (t.up) "+${t.countUp}" else null, if (t.down) "−${t.countDown}" else null).joinToString("  "),
                    style = DType.Number.copy(fontSize = 13.sp, color = D.c.faint),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        PressBox(Bands[(Rules.band(t.value) - 2).coerceAtLeast(0)], t.down, onDown) { Glyph(glyphMinus, Color.White, 3.dp) }
    }
}

@Composable
fun CheckBoxPixel(checked: Boolean, color: Color, size: Int = 42) {
    val bg by animateColorAsState(if (checked) color else color.copy(alpha = 0.16f), label = "cb")
    Box(
        Modifier.size(size.dp).clip(D.RSm).background(bg).border(2.dp, color.copy(alpha = if (checked) 0f else 0.85f), D.RSm),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Glyph(glyphCheck, Color.White, if (size >= 40) 3.dp else 2.dp)
    }
}

@Composable
private fun TaskRow(
    t: Task, checklist: List<CheckItem>, today: Long, expanded: Boolean, dim: Boolean,
    onToggle: (Boolean) -> Unit, onExpand: () -> Unit, onCheck: (CheckItem) -> Unit, onClick: () -> Unit,
) {
    val done = if (t.type == TaskType.DAILY) t.doneDay == today else t.done
    val band = Bands[Rules.band(t.value)]
    val overdue = t.type == TaskType.TODO && !t.done && t.dueDay in 0 until today
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .alpha(if (dim) 0.6f else 1f)
            .clip(D.RMd)
            .background(D.c.surface)
            .border(1.dp, D.c.line, D.RMd),
    ) {
        Row(Modifier.clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggle(!done) }) {
                CheckBoxPixel(done, if (done) D.c.faint else band, 46)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    style = DType.Strong.copy(fontSize = 15.5.sp, color = if (done) D.c.muted else D.c.ink, textDecoration = if (done && t.type == TaskType.TODO) TextDecoration.LineThrough else null),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (t.notes.isNotBlank()) Text(t.notes, style = DType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val hasMeta = t.streak > 0 || checklist.isNotEmpty() || t.reminder >= 0 || t.dueDay >= 0
                if (hasMeta) {
                    Gap(4)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (t.type == TaskType.DAILY && t.streak > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PixelImage(Sprites.flame, pixel = 1.5.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(t.streak.toString(), style = DType.Number.copy(fontSize = 13.sp, color = D.c.xp))
                            }
                        }
                        if (checklist.isNotEmpty()) {
                            Row(
                                Modifier.clip(D.RXs).background(D.c.surface2).clickable(onClick = onExpand).padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Glyph(glyphCheck, D.c.muted, 1.dp)
                                Spacer(Modifier.width(4.dp))
                                Text("${checklist.count { it.done }}/${checklist.size}", style = DType.Number.copy(fontSize = 13.sp, color = D.c.muted))
                                Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, Modifier.size(16.dp), tint = D.c.muted)
                            }
                        }
                        if (t.reminder >= 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Notifications, null, Modifier.size(14.dp), tint = D.c.faint)
                                Text(timeLabel(t.reminder), style = DType.Small)
                            }
                        }
                        if (t.type == TaskType.TODO && t.dueDay >= 0 && !t.done) {
                            Text(
                                if (overdue) stringResource(R.string.overdue, dayLabel(t.dueDay)) else stringResource(R.string.due_on, dayLabel(t.dueDay)),
                                style = DType.Small.copy(color = if (overdue) D.c.hp else D.c.faint),
                            )
                        }
                    }
                }
            }
        }
        if (expanded && checklist.isNotEmpty()) {
            Column(Modifier.padding(start = 68.dp, end = 12.dp, bottom = 10.dp)) {
                checklist.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clip(D.RXs).clickable { onCheck(item) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBoxPixel(item.done, if (item.done) D.c.faint else band, 24)
                        Spacer(Modifier.width(10.dp))
                        Text(item.text, style = DType.Body.copy(color = if (item.done) D.c.muted else D.c.ink, fontSize = 14.sp))
                    }
                }
            }
        }
    }
}

