package app.dayquest.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.CheckItem
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Difficulty
import app.dayquest.game.Rules
import app.dayquest.ui.components.Chip
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.DateDialog
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.Segmented
import app.dayquest.ui.components.TimeDialog
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.dayLabel
import app.dayquest.ui.difficultyLabel
import app.dayquest.ui.goldText
import app.dayquest.ui.locale
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import app.dayquest.ui.timeLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

private class Line(val id: Long, text: String, done: Boolean) {
    var text by mutableStateOf(text)
    var done by mutableStateOf(done)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTaskScreen(id: Long, type: TaskType, initialTitle: String, onDone: () -> Unit, onStats: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val c = context.container
    val db = c.db.game()
    var loaded by remember { mutableStateOf(id == 0L) }
    var base by remember { mutableStateOf(Task(type = type, title = initialTitle, up = true, down = type == TaskType.HABIT)) }
    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var up by remember { mutableStateOf(true) }
    var down by remember { mutableStateOf(type == TaskType.HABIT) }
    var mask by remember { mutableIntStateOf(0b1111111) }
    var every by remember { mutableIntStateOf(0) }
    var due by remember { mutableStateOf(-1L) }
    var reminder by remember { mutableIntStateOf(-1) }
    var cost by remember { mutableStateOf("10") }
    val lines = remember { mutableStateListOf<Line>() }
    var newLine by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id == 0L) return@LaunchedEffect
        val t = db.task(id) ?: return@LaunchedEffect onDone()
        base = t
        title = t.title; notes = t.notes; difficulty = t.difficulty; up = t.up; down = t.down
        mask = t.repeatMask; every = t.everyDays; due = t.dueDay; reminder = t.reminder; cost = goldText(t.cost)
        lines.clear(); lines.addAll(db.checklist(t.id).map { Line(it.id, it.text, it.done) })
        loaded = true
    }
    if (!loaded) return
    val kind = base.type

    fun save() {
        if (title.isBlank()) { titleError = true; return }
        val parsedCost = cost.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0)
        fun edited(t: Task) = t.copy(
            title = title.trim(), notes = notes.trim(), difficulty = difficulty,
            up = up || !down, down = down,
            repeatMask = if (mask == 0) 0b1111111 else mask, everyDays = every,
            dueDay = due, reminder = reminder,
            cost = parsedCost ?: t.cost,
        )
        val items = lines.filter { it.text.isNotBlank() } + listOfNotNull(newLine.takeIf { it.isNotBlank() }?.let { Line(0, it, false) })
        c.launch {
            // Re-read so a tick made while editing (streak, done day) isn't overwritten.
            val tid = if (base.id == 0L) db.insert(edited(base).copy(position = db.nextPosition(base.type), startDay = c.game.today()))
            else base.id.also { db.update(edited(db.task(base.id) ?: base)) }
            db.clearChecklist(tid)
            items.forEachIndexed { i, l -> db.insertCheck(CheckItem(taskId = tid, text = l.text.trim(), done = l.done, position = i)) }
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(D.c.bg).statusBarsPadding().imePadding()) {
        TopBar(
            stringResource(
                when {
                    base.id != 0L -> R.string.edit_task
                    kind == TaskType.HABIT -> R.string.new_habit
                    kind == TaskType.DAILY -> R.string.new_daily
                    kind == TaskType.TODO -> R.string.new_todo
                    else -> R.string.new_reward
                }
            ),
            onBack = onDone,
        ) {
            if (base.id != 0L && kind != TaskType.REWARD) IconButton(onClick = { onStats(base.id) }) {
                Icon(Icons.Rounded.BarChart, stringResource(R.string.task_stats), tint = D.c.accent)
            }
            if (base.id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = D.c.hp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.title))
            DTextField(title, { title = it; titleError = false }, placeholder = stringResource(R.string.title))
            if (titleError) Text(stringResource(R.string.title_required), style = DType.Small.copy(color = D.c.hp), modifier = Modifier.padding(4.dp))
            Gap(8)
            DTextField(notes, { notes = it }, placeholder = stringResource(R.string.notes), minLines = 2)

            if (kind == TaskType.REWARD) {
                SectionTitle(stringResource(R.string.cost))
                DTextField(cost, { cost = it }, number = true)
            } else {
                SectionTitle(stringResource(R.string.difficulty))
                Segmented(Difficulty.entries.toList(), difficulty, { difficultyLabel(it) }, { difficulty = it })
            }

            if (kind == TaskType.HABIT) {
                SectionTitle(stringResource(R.string.habit_direction))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.positive), up, { up = !up || !down }, D.c.good)
                    Chip(stringResource(R.string.negative), down, { down = !down || !up }, D.c.bad)
                }
            }

            if (kind == TaskType.DAILY) {
                SectionTitle(stringResource(R.string.repeat))
                Segmented(listOf(0, 1), if (every > 0) 1 else 0, { stringResource(if (it == 0) R.string.weekly else R.string.every_n) }, { every = if (it == 0) 0 else maxOf(every, 2) })
                Gap(10)
                if (every == 0) {
                    val loc = locale()
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { d ->
                            val bit = 1 shl (d.value - 1)
                            Chip(d.getDisplayName(TextStyle.SHORT, loc), mask and bit != 0, { mask = mask xor bit })
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { every = (every - 1).coerceAtLeast(1) }) { Icon(Icons.Rounded.Remove, null, tint = D.c.accent) }
                        Text(pluralStringResource(R.plurals.every_n_days, every, every), style = DType.Strong, modifier = Modifier.weight(1f))
                        IconButton(onClick = { every = (every + 1).coerceAtMost(60) }) { Icon(Icons.Rounded.Add, null, tint = D.c.accent) }
                    }
                }
            }

            if (kind == TaskType.TODO) {
                SectionTitle(stringResource(R.string.due_date))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(if (due >= 0) dayLabel(due) else stringResource(R.string.no_due), due >= 0, { pickDate = true })
                    if (due >= 0) Chip("✕", false, { due = -1 })
                }
            }

            if (kind == TaskType.DAILY || kind == TaskType.TODO) {
                SectionTitle(stringResource(R.string.reminder))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(if (reminder >= 0) timeLabel(reminder) else stringResource(R.string.no_reminder), reminder >= 0, { pickTime = true })
                    if (reminder >= 0) Chip("✕", false, { reminder = -1 })
                }

                SectionTitle(stringResource(R.string.checklist))
                lines.forEach { l ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DTextField(l.text, { l.text = it }, Modifier.weight(1f))
                        IconButton(onClick = { lines.remove(l) }) { Icon(Icons.Rounded.Close, null, tint = D.c.faint) }
                    }
                    Gap(6)
                }
                DTextField(
                    newLine, { newLine = it }, placeholder = stringResource(R.string.add_item),
                    onDone = { if (newLine.isNotBlank()) { lines.add(Line(0, newLine, false)); newLine = "" } },
                    trailing = {
                        IconButton(onClick = { if (newLine.isNotBlank()) { lines.add(Line(0, newLine, false)); newLine = "" } }) {
                            Icon(Icons.Rounded.Add, null, tint = D.c.accent)
                        }
                    },
                )
            }
            Gap(24)
        }
        Row(Modifier.padding(18.dp).navigationBarsPadding()) {
            PrimaryButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth())
        }
    }

    if (pickDate) DateDialog(if (due >= 0) LocalDate.ofEpochDay(due) else LocalDate.now(), { pickDate = false }) { due = it.toEpochDay() }
    if (pickTime) TimeDialog(if (reminder >= 0) reminder else 9 * 60, { pickTime = false }) { reminder = it }
    if (confirmDelete) ConfirmDialog(
        stringResource(R.string.delete_task_q), stringResource(R.string.delete_task_text), stringResource(R.string.delete),
        onConfirm = { c.launch { db.delete(base) }; onDone() }, onDismiss = { confirmDelete = false }, danger = true,
    )
}

