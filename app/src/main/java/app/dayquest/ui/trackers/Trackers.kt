package app.dayquest.ui.trackers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Tracker
import app.dayquest.data.TrackerDay
import app.dayquest.ui.components.Chip
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** «8», «7,5» – a tracker's number without a trailing zero. */
fun num(v: Double, ru: Boolean = Locale.getDefault().language == "ru"): String {
    val r = Math.round(v * 10) / 10.0
    val s = if (r == Math.floor(r)) r.toLong().toString() else r.toString()
    return if (ru) s.replace('.', ',') else s
}

private fun parse(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

/** How full a day is, 0..1, for a cell's colour. */
private fun fill(t: Tracker, v: Double?): Float = when {
    v == null || v <= 0.0 -> 0f
    t.yesNo -> 1f
    t.target <= 0 -> 1f
    else -> (v / t.target).toFloat().coerceIn(0f, 1f)
}

@Composable
private fun Cell(t: Tracker, v: Double?, modifier: Modifier, label: String? = null, today: Boolean = false) {
    val f = fill(t, v)
    val met = v != null && t.met(v)
    Box(
        modifier
            .clip(D.RSm)
            .background(if (f == 0f) D.c.surface2 else D.c.accent.copy(alpha = 0.25f + 0.75f * f))
            .then(if (today) Modifier.border(2.dp, D.c.ink, D.RSm) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) Text(label, style = DType.Small.copy(fontSize = 11.sp, color = if (f > 0.55f) Color.White else D.c.muted))
        else if (met) Text("✓", style = DType.Small.copy(fontSize = 11.sp, color = Color.White))
    }
}

/** A tracker on the board (#7): today's value and the quick button, and the last seven days as cells. */
@Composable
fun TrackerRow(t: Tracker, days: Map<Long, Double>, today: Long, onQuick: () -> Unit, onClick: () -> Unit) {
    val v = days[today] ?: 0.0
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
        Column(Modifier.weight(1f)) {
            Text(t.title, style = DType.Strong.copy(fontSize = 15.5.sp), maxLines = 1)
            Text(
                if (t.yesNo) stringResource(if (v >= 1) R.string.tracker_yes else R.string.tracker_not_yet)
                else num(v) + " / " + num(t.target) + (if (t.unit.isNotBlank()) " " + t.unit else ""),
                style = DType.Small.copy(color = if (t.met(v)) D.c.good else D.c.muted),
            )
            Gap(6)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (d in today - 6..today) Cell(t, days[d], Modifier.size(18.dp), today = d == today)
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(46.dp).clip(D.RSm).background(if (t.met(v)) D.c.good else D.c.accentSoft).clickable(onClick = onQuick),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (t.yesNo) (if (v >= 1) "✓" else "") else "+" + num(t.step), style = DType.Strong.copy(color = if (t.met(v)) Color.White else D.c.accent))
        }
    }
}

/** Writing a tracker's value for a day: a number, or yes / no. */
@Composable
fun ValueDialog(t: Tracker, day: Long, current: Double, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    var text by remember { mutableStateOf(if (current > 0) num(current) else "") }
    fun put(v: Double) { c.launch { c.game.setTracker(t.id, day, v) }; onDismiss() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        title = { Text(app.dayquest.ui.dayLabel(day), style = DType.Heading) },
        text = {
            Column {
                Text(t.title, style = DType.Strong)
                Gap(10)
                if (t.yesNo) {
                    PrimaryButton(stringResource(R.string.tracker_done_q), { put(1.0) }, Modifier.fillMaxWidth())
                    Gap(8)
                    SoftButton(stringResource(R.string.tracker_not_done), { put(0.0) }, Modifier.fillMaxWidth())
                } else {
                    DTextField(text, { text = it.take(8) }, placeholder = num(t.target) + " " + t.unit, number = true, onDone = { parse(text)?.let(::put) })
                    Gap(12)
                    PrimaryButton(stringResource(R.string.save), { put(parse(text) ?: 0.0) }, Modifier.fillMaxWidth())
                }
            }
        },
    )
}

/** Making a tracker, or changing one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackerDialog(old: Tracker?, onDismiss: () -> Unit, onDelete: () -> Unit = {}) {
    val c = LocalContext.current.container
    var title by remember { mutableStateOf(old?.title ?: "") }
    var kind by remember { mutableIntStateOf(old?.kind ?: 0) }
    var unit by remember { mutableStateOf(old?.unit ?: "") }
    var target by remember { mutableStateOf(old?.target?.let { num(it) } ?: "") }
    var step by remember { mutableStateOf(old?.step?.let { num(it) } ?: "1") }
    var deleting by remember { mutableStateOf(false) }
    val ok = title.isNotBlank() && (kind == 1 || (parse(target) ?: 0.0) > 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        title = { Text(stringResource(if (old == null) R.string.tracker_new else R.string.tracker_edit), style = DType.Heading) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DTextField(title, { title = it.take(40) }, placeholder = stringResource(R.string.tracker_name_hint))
                Gap(10)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.tracker_number), kind == 0, { kind = 0 })
                    Chip(stringResource(R.string.tracker_yesno), kind == 1, { kind = 1 })
                }
                if (kind == 0) {
                    Gap(10)
                    Text(stringResource(R.string.tracker_target), style = DType.Small)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DTextField(target, { target = it.take(8) }, Modifier.weight(1f), placeholder = "8", number = true)
                        DTextField(unit, { unit = it.take(12) }, Modifier.weight(1f), placeholder = stringResource(R.string.tracker_unit_hint))
                    }
                    Gap(10)
                    Text(stringResource(R.string.tracker_step), style = DType.Small)
                    DTextField(step, { step = it.take(6) }, placeholder = "1", number = true)
                }
                Gap(16)
                PrimaryButton(stringResource(R.string.save), {
                    val t = (old ?: Tracker(title = "")).copy(
                        title = title.trim(), kind = kind, unit = if (kind == 0) unit.trim() else "",
                        target = if (kind == 0) parse(target) ?: 1.0 else 1.0, step = if (kind == 0) (parse(step) ?: 1.0).coerceAtLeast(0.1) else 1.0,
                    )
                    c.launch { val db = c.db.game(); if (old == null) db.insertTracker(t.copy(position = db.nextTrackerPosition())) else db.updateTracker(t) }
                    onDismiss()
                }, Modifier.fillMaxWidth(), enabled = ok)
                if (old != null) {
                    Gap(8)
                    SoftButton(stringResource(R.string.delete), { deleting = true }, Modifier.fillMaxWidth(), color = D.c.hp, soft = D.c.hpSoft)
                }
            }
        },
    )
    if (deleting && old != null) ConfirmDialog(stringResource(R.string.tracker_delete_t), stringResource(R.string.tracker_delete_b), stringResource(R.string.delete), {
        c.launch { c.db.game().deleteTracker(old.id) }
        onDelete()
    }, { deleting = false }, danger = true)
}

/** One tracker: a month of days as a grid, the average and the days the target was met; any day can be put right. */
@Composable
fun TrackerScreen(id: Long, today: Long, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val t by remember(id) { c.db.game().observeTracker(id) }.collectAsStateWithLifecycle(initialValue = null)
    val history by remember(id) { c.db.game().observeTrackerHistory(id) }.collectAsStateWithLifecycle(initialValue = emptyList<TrackerDay>())
    var month by remember { mutableLongStateOf(LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()) }
    var open by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf(false) }
    val tr = t ?: return
    val values = history.associate { it.day to it.value }
    val first = LocalDate.ofEpochDay(month)
    val len = first.lengthOfMonth()
    val locale = Locale.getDefault()
    val inMonth = (0 until len).map { month + it }.filter { it <= today }
    val logged = inMonth.mapNotNull { d -> values[d]?.takeIf { it > 0 } }
    val metDays = inMonth.count { d -> values[d]?.let { tr.met(it) } == true }

    Column(Modifier.fillMaxSize().background(D.c.bg).statusBarsPadding()) {
        TopBar(tr.title, onBack) { IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, stringResource(R.string.tracker_edit), tint = D.c.ink) } }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = first.minusMonths(1).toEpochDay() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = D.c.accent) }
                Text(first.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } + " " + first.year,
                    style = DType.Strong, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (first.plusMonths(1).toEpochDay() <= today) month = first.plusMonths(1).toEpochDay() }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (first.plusMonths(1).toEpochDay() <= today) D.c.accent else D.c.line)
                }
            }
            Gap(6)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                java.time.DayOfWeek.entries.forEach { d ->
                    Text(d.getDisplayName(TextStyle.SHORT, locale), style = DType.Small.copy(fontSize = 11.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Gap(4)
            val lead = first.dayOfWeek.value - 1
            val rows = (lead + len + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val n = r * 7 + col - lead
                        if (n !in 0 until len) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else {
                            val d = month + n
                            Cell(tr, values[d], Modifier.weight(1f).aspectRatio(1f).then(if (d <= today) Modifier.clickable { open = d } else Modifier),
                                label = "${n + 1}", today = d == today)
                        }
                    }
                }
            }
            Gap(14)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(if (tr.yesNo) "${logged.size}" else if (logged.isEmpty()) "–" else num(logged.average()) + (if (tr.unit.isNotBlank()) " " + tr.unit else ""),
                    stringResource(if (tr.yesNo) R.string.tracker_days_yes else R.string.tracker_average), Modifier.weight(1f))
                Stat("$metDays / ${inMonth.size}", stringResource(R.string.tracker_met_days), Modifier.weight(1f))
            }
            Gap(10)
            Text(stringResource(R.string.tracker_screen_hint), style = DType.Small)
            Gap(24)
        }
    }
    open?.let { d -> ValueDialog(tr, d, values[d] ?: 0.0) { open = null } }
    if (editing) TrackerDialog(tr, { editing = false }, onDelete = { editing = false; onBack() })
}

@Composable
private fun Stat(big: String, small: String, modifier: Modifier) {
    Column(modifier.clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd).padding(12.dp)) {
        Text(big, style = DType.Number.copy(fontSize = 20.sp))
        Text(small, style = DType.Small)
    }
}

