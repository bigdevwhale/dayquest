package app.dayquest.ui.tasks

import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import app.dayquest.ui.components.Chip
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
import app.dayquest.ui.journal.MoodCard

private enum class Board { HABITS, DAILIES, TODOS }

val ManaBlue = Color(0xFF4F9BEF)
private val BossColor = Color(0xFFB45CFF)

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
fun TasksScreen(hero: Hero, onChest: () -> Unit, onEdit: (Long) -> Unit, onNew: (TaskType, String) -> Unit, onSettings: () -> Unit, onFocus: () -> Unit, onHome: () -> Unit) {
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

    // The player's own tags filter the board; the row appears once any task has one.
    val allTags = tasks.flatMap { it.tagList }.distinct().sortedBy { it.lowercase() }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    if (tag != null && tag !in allTags) tag = null
    val shown = tag?.let { tg -> tasks.filter { tg in it.tagList } } ?: tasks
    val habits = shown.filter { it.type == TaskType.HABIT }
    val dailies = shown.filter { it.type == TaskType.DAILY }
    val dueDailies = dailies.filter { Rules.isDue(it, today) }
    val todos = shown.filter { it.type == TaskType.TODO }

    val actions = remember(fb) { Actions(c, fb, context) }
    // habit goals count from the start of the month (or of the week, when that is earlier) (#6)
    val goalFrom = minOf(Rules.goalStart(today, 7), Rules.goalStart(today, 30))
    val ups by remember(goalFrom) { c.db.game().observeUps(goalFrom) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val mood by remember(today) { c.db.game().observeJournalDay(today) }.collectAsStateWithLifecycle(initialValue = null)

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // Stuck at the top, the tabs keep clear of the status bar instead of sliding under the clock.
    val stuck by remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex >= 1 } }
    // Once the scene has gone up, a strip of the page's colour keeps the board from running under the clock.
    val scrolled by remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300 } }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { HeroHeader(hero, fx, onSettings, onChest, onFocus, onHome) }
        stickyHeader(key = "tabs") {
            Column(Modifier.fillMaxWidth().background(D.c.bg).then(if (stuck) Modifier.statusBarsPadding() else Modifier).padding(horizontal = 12.dp, vertical = 8.dp)) {
                BoardTabs(board, { board = it },
                    habits = habits.size,
                    dailiesDone = dueDailies.count { d -> d.doneDay == today }, dailiesDue = dueDailies.size,
                    todosOpen = todos.count { t -> !t.done })
                if (allTags.isNotEmpty()) {
                    Gap(8)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(stringResource(R.string.tag_all), tag == null, { tag = null }, small = true)
                        allTags.forEach { tg -> Chip("#$tg", tag == tg, { tag = if (tag == tg) null else tg }, small = true) }
                    }
                }
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
                onAdd = { title -> c.launch { quickAdd(c, board.type, title, today, tag.orEmpty()) } },
                onMore = { onNew(board.type, it) },
            )
        }
        when (board) {
            Board.HABITS -> {
                if (habits.isEmpty()) item { Empty(stringResource(R.string.empty_habits)) }
                items(habits, key = { it.id }) { t ->
                    val progress = if (t.goal > 0) Rules.goalStart(today, t.goalPer).let { from -> ups.count { u -> u.taskId == t.id && u.day >= from } } else 0
                    SwipeRow(
                        right = if (t.up) ("+" to { actions.habit(t, true) }) else null,
                        left = if (t.down) ("−" to { actions.habit(t, false) }) else null,
                    ) { HabitRow(t, progress, onClick = { onEdit(t.id) }, onUp = { actions.habit(t, true) }, onDown = { actions.habit(t, false) }) }
                }
            }
            Board.DAILIES -> {
                if (dailies.isEmpty()) item { Empty(stringResource(R.string.empty_dailies)) }
                val open = dueDailies.filter { it.doneDay != today }
                val done = dueDailies.filter { it.doneDay == today }
                if (dueDailies.isNotEmpty() && done.size == dueDailies.size) item(key = "progress") { DayProgress(done.size, dueDailies.size) }
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
        // The day's mood waits at the end of the board, after the tasks.
        item(key = "mood") { MoodCard(today, mood, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) }
    }
    if (scrolled) Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(D.c.bg))
    }
}

private val Board.type get() = when (this) {
    Board.HABITS -> TaskType.HABIT
    Board.DAILIES -> TaskType.DAILY
    Board.TODOS -> TaskType.TODO
}

suspend fun quickAdd(c: app.dayquest.AppContainer, type: TaskType, title: String, today: Long, tags: String = "") {
    val db = c.db.game()
    db.insert(Task(type = type, title = title.trim(), position = db.nextPosition(type), startDay = today, up = true, down = type == TaskType.HABIT, tags = tags))
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
            if (o.room > 0 && !o.dungeonDone) add(s(R.string.room_cleared, o.room, goldText(o.roomGold), o.roomItems.joinToString(", ") { context.itemLabel(it) }))
            if (o.goal) add(s(R.string.snack_goal))
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

    /** A to-do moves to tomorrow (swiped to the left). */
    fun tomorrow(t: Task, today: Long) {
        fb.tap()
        c.launch { c.db.game().update(t.copy(dueDay = today + 1)) }
        fb.show(s(R.string.moved_tomorrow), null, 0)
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
        val done = if (t.type == TaskType.DAILY) t.doneDay == today else t.done
        SwipeRow(
            right = (if (done) "↺" else "✓") to { if (t.type == TaskType.DAILY) actions.daily(t, !done) else actions.todo(t, !done) },
            left = if (t.type == TaskType.TODO && !t.done) ("→" to { actions.tomorrow(t, today) }) else ("✎" to { onEdit(t.id) }),
        ) {
        TaskRow(
            t, items, today, expanded = t.id in expanded, dim = dim,
            onToggle = { done -> if (t.type == TaskType.DAILY) actions.daily(t, done) else actions.todo(t, done) },
            onExpand = { if (t.id in expanded) expanded.remove(t.id) else expanded.add(t.id) },
            onCheck = actions::check,
            onClick = { onEdit(t.id) },
        )
        }
    }
}

/**
 * The three boards as tabs (#UI): an icon, the name and a little count each; the chosen one is filled with the
 * accent colour, and the day's dailies turn green once all are done.
 */
@Composable
private fun BoardTabs(board: Board, onSelect: (Board) -> Unit, habits: Int, dailiesDone: Int, dailiesDue: Int, todosOpen: Int) {
    Row(
        Modifier.fillMaxWidth().clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (b in Board.entries) {
            val on = b == board
            val bg by animateColorAsState(if (on) D.c.accent else Color.Transparent, label = "tab")
            val fg = if (on) D.c.onAccent else D.c.muted
            val allDone = b == Board.DAILIES && dailiesDue > 0 && dailiesDone >= dailiesDue
            val count = when (b) {
                Board.HABITS -> if (habits > 0) "$habits" else null
                Board.DAILIES -> if (dailiesDue > 0) "$dailiesDone/$dailiesDue" else null
                Board.TODOS -> if (todosOpen > 0) "$todosOpen" else null
            }
            Column(
                Modifier.weight(1f).clip(D.RSm).background(bg).clickable { onSelect(b) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (b) {
                        Board.HABITS -> Glyph(glyphPlus, fg, 1.5.dp)
                        Board.DAILIES -> PixelImage(Sprites.flame, pixel = 1.25.dp, alpha = if (on) 1f else 0.7f)
                        Board.TODOS -> Glyph(glyphCheck, fg, 1.5.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(when (b) { Board.HABITS -> R.string.tab_habits; Board.DAILIES -> R.string.tab_dailies; Board.TODOS -> R.string.tab_todos }),
                        style = DType.Strong.copy(fontSize = 14.sp, color = if (on) D.c.onAccent else D.c.ink), maxLines = 1,
                    )
                }
                if (count != null) Text(
                    count,
                    style = DType.Number.copy(fontSize = 12.sp, color = when { allDone && on -> D.c.onAccent; allDone -> D.c.good; else -> fg }),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

/**
 * A row that can be swiped: to the right for [right] (done, or + for a habit), to the left for [left]
 * (a to-do to tomorrow, a habit's −, or the editor). The label shows underneath as the row moves; it springs back.
 */
@Composable
private fun SwipeRow(right: Pair<String, () -> Unit>?, left: Pair<String, () -> Unit>?, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val dx = remember { androidx.compose.animation.core.Animatable(0f) }
    val density = LocalDensity.current
    val limit = with(density) { 96.dp.toPx() }
    val r by rememberUpdatedState(right)
    val l by rememberUpdatedState(left)
    Box(Modifier.fillMaxWidth()) {
        val shown = if (dx.value > 0) r else if (dx.value < 0) l else null
        if (shown != null) Box(
            Modifier.matchParentSize().padding(horizontal = 16.dp, vertical = 5.dp).clip(D.RMd)
                .background(if (dx.value > 0) D.c.good.copy(alpha = (dx.value / limit).coerceIn(0.25f, 1f)) else D.c.accent.copy(alpha = (-dx.value / limit).coerceIn(0.25f, 1f))),
            contentAlignment = if (dx.value > 0) Alignment.CenterStart else Alignment.CenterEnd,
        ) { Text(shown.first, style = DType.Heading.copy(color = Color.White, fontSize = 22.sp), modifier = Modifier.padding(horizontal = 22.dp)) }
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(dx.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val v = dx.value
                            if (v > limit) r?.second?.invoke() else if (v < -limit) l?.second?.invoke()
                            scope.launch { dx.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f)) }
                        },
                        onDragCancel = { scope.launch { dx.animateTo(0f) } },
                    ) { change, amount ->
                        val next = dx.value + amount
                        if ((next > 0 && r == null) || (next < 0 && l == null)) return@detectHorizontalDragGestures
                        change.consume()
                        scope.launch { dx.snapTo(next.coerceIn(-limit * 1.4f, limit * 1.4f)) }
                    }
                },
        ) { content() }
    }
}

/** One to three little stars for how hard a task is (a trivial one has none): what it pays in experience and mana. */
@Composable
private fun DifficultyMarks(d: app.dayquest.game.Difficulty, color: Color) {
    val n = when (d) { app.dayquest.game.Difficulty.TRIVIAL -> 0; app.dayquest.game.Difficulty.EASY -> 1; app.dayquest.game.Difficulty.MEDIUM -> 2; app.dayquest.game.Difficulty.HARD -> 3 }
    if (n > 0) Text("★".repeat(n), style = DType.Small.copy(fontSize = 11.sp, color = color, letterSpacing = 1.sp))
}

// ---- Header ------------------------------------------------------------------------------

@Composable
private fun HeroHeader(hero: Hero, fx: SceneFx, onSettings: () -> Unit, onChest: () -> Unit, onFocus: () -> Unit, onHome: () -> Unit) {
    val boss = Catalog.boss(hero.quest)
    val pets = app.dayquest.ui.pets.rememberPets()
    Column(Modifier.fillMaxWidth()) {
        Box {
            SceneView(
                app.dayquest.ui.pets.heroScene(hero, pets, boss),
                bursts = fx.bursts, floaters = fx.floaters, onPet = app.dayquest.ui.pets.rememberPat(hero).takeIf { hero.pet.isNotEmpty() },
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
                // Home is one tap away: the pets that live there are waiting.
                Box(
                    Modifier.size(40.dp).clip(D.RSm).background(Color(0x55000000)).clickable(onClick = onHome),
                    contentAlignment = Alignment.Center,
                ) { PixelImage(app.dayquest.pixel.Badges.house, pixel = 2.dp) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(40.dp).clip(D.RSm).background(Color(0x55000000)).clickable(onClick = onFocus),
                    contentAlignment = Alignment.Center,
                ) { PixelImage(app.dayquest.pixel.Badges.hourglass, pixel = 2.dp) }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettings, modifier = Modifier.size(40.dp).clip(D.RSm).background(Color(0x55000000))) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = Color.White)
                }
            }
        }
        StatsPanel(hero, compact = true)
    }
}

@Composable
fun StatsPanel(hero: Hero, modifier: Modifier = Modifier, compact: Boolean = false) {
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
            Column(Modifier.weight(1f)) {
                Text(hero.name, style = DType.Heading.copy(fontSize = 21.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (hero.title.isNotEmpty()) Text(app.dayquest.ui.pets.titleName(hero.title), style = DType.Small.copy(color = D.c.gold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PixelImage(Sprites.coin, pixel = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text(goldText(hero.gold), style = DType.Number.copy(color = D.c.gold, fontSize = 17.sp))
        }
        val mana = if (app.dayquest.game.Classes.of(hero) != null) app.dayquest.game.Classes.maxMp(LocalContext.current.container.game.stats(hero)) else null
        if (compact) {
            // On the task board the hero's bars sit side by side, so the tasks start higher.
            Gap(8)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Sprites.heart, hero.hp / Rules.MAX_HP, D.c.hp, "${num(hero.hp.coerceAtLeast(0.0))}/${Rules.MAX_HP.toInt()}", Modifier.weight(1f))
                MiniStat(Sprites.star, (hero.xp / next).toFloat(), D.c.xp, "${num(hero.xp)}/$next", Modifier.weight(1f))
                if (mana != null) MiniStat(app.dayquest.pixel.Badges.mana, (hero.mp / mana).toFloat(), ManaBlue, "${num(hero.mp.coerceAtMost(mana))}/${num(mana)}", Modifier.weight(1f))
            }
        } else {
        Gap(10)
        StatLine(Sprites.heart, hero.hp / Rules.MAX_HP, D.c.hp, "${num(hero.hp.coerceAtLeast(0.0))} / ${Rules.MAX_HP.toInt()}")
        Gap(6)
        StatLine(Sprites.star, (hero.xp / next).toFloat(), D.c.xp, "${num(hero.xp)} / $next")
        if (mana != null) {
            Gap(6)
            StatLine(app.dayquest.pixel.Badges.mana, (hero.mp / mana).toFloat(), ManaBlue, "${num(hero.mp.coerceAtMost(mana))} / ${num(mana)}")
        }
        }
        if (boss != null && compact) {
            Gap(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) { PixelImage(Sprites.boss(boss.shape, boss.primary, boss.secondary), pixel = 0.4.dp) }
                Spacer(Modifier.width(6.dp))
                Text(app.dayquest.ui.bossName(boss), style = DType.Small.copy(color = BossColor, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                PixelBar((hero.questHp / boss.hp).toFloat(), BossColor, Modifier.width(90.dp))
                Spacer(Modifier.width(6.dp))
                Text("${num(hero.questHp)}/${num(boss.hp)}", style = DType.Number.copy(fontSize = 12.sp, color = D.c.muted))
            }
        } else if (boss != null) {
            // The foe's bar sits apart from the hero's own, under its name, so it never reads as a fourth hero stat.
            Gap(10)
            androidx.compose.material3.HorizontalDivider(color = D.c.line)
            Gap(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { PixelImage(Sprites.boss(boss.shape, boss.primary, boss.secondary), pixel = 0.5.dp) }
                Spacer(Modifier.width(8.dp))
                Text(app.dayquest.ui.bossName(boss), style = DType.Small.copy(color = BossColor), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${num(hero.questHp)} / ${num(boss.hp)}", style = DType.Number.copy(fontSize = 13.sp, color = D.c.muted))
            }
            Gap(4)
            PixelBar((hero.questHp / boss.hp).toFloat(), BossColor)
        }
    }
}

/** A hero bar for the compact panel: the icon, a short bar, and the numbers under it. */
@Composable
private fun MiniStat(icon: app.dayquest.pixel.Sprite, fraction: Number, color: Color, label: String, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelImage(icon, pixel = 1.5.dp)
            Spacer(Modifier.width(5.dp))
            PixelBar(fraction.toFloat(), color, Modifier.weight(1f))
        }
        Text(label, style = DType.Number.copy(fontSize = 11.sp, color = D.c.muted), maxLines = 1, modifier = Modifier.padding(start = 2.dp, top = 1.dp))
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
                    // «…» opens the full editor with what's typed; «+» adds it as is.
                    IconButton(onClick = { onMore(text); text = "" }) { Icon(Icons.Rounded.MoreHoriz, stringResource(R.string.add_more), tint = D.c.muted) }
                    IconButton(onClick = { if (text.isNotBlank()) { onAdd(text); text = "" } else onMore("") }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = if (text.isNotBlank()) D.c.accent else D.c.faint)
                    }
                }
            },
        )
    }
}

/** How far today's dailies are: a bar that fills, and a word of praise when it's full. */
@Composable
private fun DayProgress(done: Int, total: Int) {
    val all = done >= total
    val f by animateFloatAsState(done.toFloat() / total, label = "day")
    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(8.dp).clip(D.RXs).background(D.c.surface2).border(1.dp, D.c.line, D.RXs)) {
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(if (all) D.c.good else D.c.xp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (all) stringResource(R.string.dailies_all_done) else "$done/$total",
            style = DType.Small.copy(color = if (all) D.c.good else D.c.muted),
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
private fun HabitRow(t: Task, progress: Int, onClick: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    val band = Bands[Rules.band(t.value)]
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(D.RMd)
            .background(D.c.surface)
            .border(1.dp, D.c.line, D.RMd)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressBox(band, t.up, onUp, size = 38) { Glyph(glyphPlus, Color.White, 3.dp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = DType.Strong.copy(fontSize = 15.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (t.notes.isNotBlank()) Text(t.notes, style = DType.Muted.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DifficultyMarks(t.difficulty, D.c.gold)
                if (t.tags.isNotBlank()) Text(t.tagList.joinToString(" ") { "#$it" }, style = DType.Small.copy(color = D.c.accent), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // the goal for the week or month, and how far along it is (#6)
            if (t.goal > 0) Text(
                stringResource(if (t.goalPer == 30) R.string.goal_progress_month else R.string.goal_progress_week, minOf(progress, t.goal), t.goal) + if (progress >= t.goal) " ✓" else "",
                style = DType.Small.copy(color = if (progress >= t.goal) D.c.good else D.c.accent),
            )
            if (t.countUp + t.countDown > 0) {
                Gap(2)
                Text(
                    listOfNotNull(if (t.up) "+${t.countUp}" else null, if (t.down) "−${t.countDown}" else null).joinToString("  "),
                    style = DType.Number.copy(fontSize = 13.sp, color = D.c.faint),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        PressBox(Bands[(Rules.band(t.value) - 2).coerceAtLeast(0)], t.down, onDown, size = 38) { Glyph(glyphMinus, Color.White, 3.dp) }
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
        Row(Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggle(!done) }) {
                CheckBoxPixel(done, if (done) D.c.faint else band, 36)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    style = DType.Strong.copy(fontSize = 15.sp, color = if (done) D.c.muted else D.c.ink, textDecoration = if (done && t.type == TaskType.TODO) TextDecoration.LineThrough else null),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (t.notes.isNotBlank()) Text(t.notes, style = DType.Muted.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val hasMeta = t.streak > 0 || checklist.isNotEmpty() || t.reminder >= 0 || t.dueDay >= 0 || t.tags.isNotBlank() || t.difficulty != app.dayquest.game.Difficulty.TRIVIAL
                if (hasMeta) {
                    Gap(2)
                    // difficulty, streak, checklist, reminder, due date and tags: one thin line
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DifficultyMarks(t.difficulty, if (done) D.c.faint else D.c.gold)
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
                                if (t.type == TaskType.TODO && t.dueDay >= 0 && !t.done && !overdue) Text(" · " + dayLabel(t.dueDay), style = DType.Small)
                            }
                        }
                        if (t.type == TaskType.TODO && t.dueDay >= 0 && !t.done) {
                            Text(
                                if (overdue) stringResource(R.string.overdue, dayLabel(t.dueDay)) else stringResource(R.string.due_on, dayLabel(t.dueDay)),
                                style = DType.Small.copy(color = if (overdue) D.c.hp else D.c.faint),
                            )
                        }
                        if (t.tags.isNotBlank()) Text(t.tagList.joinToString(" ") { "#$it" }, style = DType.Small.copy(color = D.c.accent), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

