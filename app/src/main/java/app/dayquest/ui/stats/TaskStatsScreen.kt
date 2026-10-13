package app.dayquest.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.LogEntry
import app.dayquest.data.LogKind
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Rules
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.locale
import app.dayquest.ui.theme.Bands
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

private val DONE_KINDS = setOf(LogKind.UP, LogKind.DAILY, LogKind.TODO)
private val MIXED = Color(0xFFF2C14E)

/** One task's story: how often, how steady, on which days, and how its strength moved. */
@Composable
fun TaskStatsScreen(id: Long, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val task by remember(id) { c.db.game().observeTask(id) }.collectAsStateWithLifecycle(initialValue = null)
    val log by remember(id) { c.db.game().observeTaskLog(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var today by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { today = c.game.today() }
    val t = task
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(t?.title ?: stringResource(R.string.task_stats), onBack)
        if (t == null || today == 0L) return@Column
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val done = log.filter { it.kind in DONE_KINDS }
            if (done.isEmpty() && log.none { it.kind == LogKind.DOWN || it.kind == LogKind.MISSED }) {
                Text(stringResource(R.string.stats_empty), style = DType.Sub, modifier = Modifier.padding(vertical = 12.dp))
            }
            Tiles(t, log, today)
            if (t.type != TaskType.TODO && t.type != TaskType.REWARD) {
                SectionTitle(stringResource(R.string.stats_month))
                MonthStrip(t, log, today)
            }
            if (done.isNotEmpty()) {
                SectionTitle(stringResource(R.string.stats_weekdays))
                Weekdays(done)
            }
            if (t.type != TaskType.REWARD && log.any { it.valueDelta != 0.0 }) {
                SectionTitle(stringResource(R.string.stats_value))
                ValueChart(t, log, today)
            }
            Gap(28)
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd).padding(12.dp)) {
        Text(value, style = DType.Number.copy(fontSize = 22.sp))
        Text(label, style = DType.Small, maxLines = 2)
    }
}

/** Headline numbers: a few stat tiles beat a chart for single values. */
@Composable
private fun Tiles(t: Task, log: List<LogEntry>, today: Long) {
    val done = log.count { it.kind in DONE_KINDS }
    val month = log.filter { it.day > today - 30 }
    val rate: String = when (t.type) {
        TaskType.DAILY -> {
            val start = maxOf(today - 29, t.startDay)
            val due = (start..today).count { Rules.isDue(t, it) && (it < today || t.doneDay == today) }
            val hit = month.filter { it.kind == LogKind.DAILY }.map { it.day }.distinct().size
            if (due == 0) "—" else "${(100 * hit / due).coerceAtMost(100)}%"
        }
        TaskType.HABIT -> month.count { it.kind == LogKind.UP }.toString()
        else -> "—"
    }
    Gap(4)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(stringResource(R.string.stats_total), done.toString(), Modifier.weight(1f))
        Tile(stringResource(R.string.stats_rate), rate, Modifier.weight(1f))
        when (t.type) {
            TaskType.DAILY -> Tile(stringResource(R.string.stats_best), "${t.streak} / ${t.bestStreak}", Modifier.weight(1f))
            TaskType.HABIT -> Tile(stringResource(R.string.stats_slips), log.count { it.kind == LogKind.DOWN }.toString(), Modifier.weight(1f))
            else -> Spacer(Modifier.weight(1f))
        }
    }
}

/** Thirty days, one cell each: done, missed (or slipped), or nothing due. */
@Composable
private fun MonthStrip(t: Task, log: List<LogEntry>, today: Long) {
    val byDay = log.groupBy { it.day }
    val good = D.c.good
    val bad = D.c.hp
    val idle = D.c.line
    val none = D.c.surface2
    DCard {
        Canvas(Modifier.fillMaxWidth().aspectRatio(15f / 2f)) {
            val cell = size.width / 15f
            val gap = 2.dp.toPx()
            for (k in 0 until 30) {
                val day = today - 29 + k
                val e = byDay[day].orEmpty()
                val ups = e.count { it.kind in DONE_KINDS }
                val downs = e.count { it.kind == LogKind.DOWN || it.kind == LogKind.MISSED }
                val color = when {
                    ups > 0 && downs == 0 -> good
                    downs > 0 && ups == 0 -> bad
                    ups > 0 -> MIXED
                    t.type == TaskType.DAILY && !Rules.isDue(t, day) -> none
                    else -> idle
                }
                drawRoundRect(color, Offset((k % 15) * cell + gap / 2, (k / 15) * cell + gap / 2), Size(cell - gap, cell - gap), CornerRadius(4.dp.toPx()))
            }
        }
        Gap(8)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Key(good, stringResource(R.string.stats_done))
            Spacer(Modifier.width(14.dp))
            Key(bad, stringResource(if (t.type == TaskType.HABIT) R.string.stats_slips else R.string.stats_missed))
            // Days with both a success and a slip get their own colour, so name it when it appears.
            val mixed = (0 until 30).any { k -> byDay[today - 29 + k].orEmpty().let { e -> e.any { it.kind in DONE_KINDS } && e.any { it.kind == LogKind.DOWN || it.kind == LogKind.MISSED } } }
            if (mixed) {
                Spacer(Modifier.width(14.dp))
                Key(MIXED, stringResource(R.string.stats_mixed))
            }
        }
    }
}

@Composable
private fun Key(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(D.RXs).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = DType.Small)
    }
}

/** Completions by weekday: one hue, bars anchored to the baseline, counts as direct labels. */
@Composable
private fun Weekdays(done: List<LogEntry>) {
    val counts = DayOfWeek.entries.map { d -> done.count { LocalDate.ofEpochDay(it.day).dayOfWeek == d } }
    val max = counts.max().coerceAtLeast(1)
    val bar = D.c.accent
    val loc = locale()
    DCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEachIndexed { i, d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(counts[i].toString(), style = DType.Number.copy(fontSize = 12.sp, color = D.c.muted))
                    Gap(2)
                    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                        val h = size.height * counts[i] / max
                        if (h > 0) drawRoundRect(bar, Offset(size.width * 0.15f, size.height - h), Size(size.width * 0.7f, h), CornerRadius(4.dp.toPx()))
                    }
                    Gap(4)
                    Text(d.getDisplayName(TextStyle.SHORT, loc), style = DType.Small.copy(fontSize = 11.sp), textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

/**
 * The task's strength (its Habitica value) at the end of each of the last 90 days, rebuilt by walking
 * the log backwards from today's value. A single series, so no legend: the title names it.
 */
@Composable
private fun ValueChart(t: Task, log: List<LogEntry>, today: Long) {
    val days = 90
    val values = remember(t.value, log, today) {
        val deltas = log.groupBy { it.day }.mapValues { (_, l) -> l.sumOf { it.valueDelta } }
        val out = DoubleArray(days)
        var v = t.value
        for (k in days - 1 downTo 0) {
            out[k] = v
            v -= deltas[today - (days - 1 - k)] ?: 0.0
        }
        out
    }
    val line = Bands[Rules.band(t.value)]
    val axis = D.c.line
    DCard {
        Text(stringResource(R.string.stats_value_text), style = DType.Muted)
        Gap(10)
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val lo = minOf(values.min(), 0.0)
            val hi = maxOf(values.max(), 1.0)
            fun y(v: Double) = (size.height * (1 - (v - lo) / (hi - lo))).toFloat()
            // Recessive zero line: above it the task is doing well, below it is slipping.
            drawLine(axis, Offset(0f, y(0.0)), Offset(size.width, y(0.0)), strokeWidth = 1.dp.toPx())
            val step = size.width / (days - 1)
            val path = androidx.compose.ui.graphics.Path()
            values.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * step, y(v)) }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
            drawCircle(line, 4.dp.toPx(), Offset(size.width, y(values.last())))
        }
    }
}
