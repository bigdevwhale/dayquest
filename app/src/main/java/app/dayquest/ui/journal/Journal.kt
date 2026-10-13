package app.dayquest.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.JournalEntry
import app.dayquest.data.LogKind
import app.dayquest.pixel.Badges
import app.dayquest.ui.LocalFeedback
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.DTextField
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.dayLabel
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import java.util.Locale

private val MOODS = listOf(R.string.mood_1, R.string.mood_2, R.string.mood_3, R.string.mood_4, R.string.mood_5)

@Composable
fun moodLabel(m: Int) = stringResource(MOODS[(m - 1).coerceIn(0, 4)])

/** Five faces in a row: tap one to set the day's mood. */
@Composable
private fun MoodPicker(selected: Int?, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { m ->
            val on = m == selected
            // Equal cells, so the row fits a narrow dialog as well as the full-width card.
            Column(
                Modifier.weight(1f).clip(D.RSm).clickable { onPick(m) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 2.dp).clip(D.RSm).background(if (on) D.c.accentSoft else D.c.surface2)
                        .border(if (on) 2.dp else 0.dp, if (on) D.c.accent else Color.Transparent, D.RSm),
                    contentAlignment = Alignment.Center,
                ) { PixelImage(Badges.mood(m), pixel = 3.dp, alpha = if (selected == null || on) 1f else 0.55f) }
                Gap(2)
                Text(
                    moodLabel(m), style = DType.Small.copy(fontSize = 11.sp, color = if (on) D.c.accent else D.c.faint),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The task board's question: how was today? A mood, and a line if you like. */
@Composable
fun MoodCard(today: Long, entry: JournalEntry?, modifier: Modifier = Modifier) {
    val c = LocalContext.current.container
    val fb = LocalFeedback.current
    var editing by remember { mutableStateOf(false) }
    DCard(modifier, padding = PaddingValues(12.dp)) {
        Text(stringResource(R.string.mood_q), style = DType.Strong)
        Gap(8)
        MoodPicker(entry?.mood) { m ->
            fb.tap()
            c.launch { c.db.game().saveJournal(JournalEntry(today, m, entry?.note.orEmpty())) }
        }
        if (entry != null) {
            Gap(8)
            Text(
                entry.note.ifBlank { stringResource(R.string.journal_note_hint) },
                style = if (entry.note.isBlank()) DType.Muted else DType.Body.copy(fontSize = 14.sp),
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clip(D.RXs).background(D.c.surface2).clickable { editing = true }.padding(10.dp),
            )
        }
    }
    if (editing && entry != null) EntryDialog(entry, onDismiss = { editing = false })
}

/** Edits one day's mood and note. */
@Composable
private fun EntryDialog(entry: JournalEntry, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    var mood by remember { mutableIntStateOf(entry.mood) }
    var note by remember { mutableStateOf(entry.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = D.c.bg,
        shape = D.RLg,
        confirmButton = {},
        title = { Text(dayLabel(entry.day), style = DType.Heading) },
        text = {
            Column {
                MoodPicker(mood) { mood = it }
                Gap(12)
                DTextField(note, { note = it.take(500) }, placeholder = stringResource(R.string.journal_note_hint), minLines = 3)
                Gap(16)
                PrimaryButton(stringResource(R.string.save), {
                    c.launch { c.db.game().saveJournal(entry.copy(mood = mood, note = note.trim())) }
                    onDismiss()
                }, Modifier.fillMaxWidth())
                Gap(8)
                SoftButton(stringResource(R.string.delete), {
                    c.launch { c.db.game().deleteJournal(entry.day) }
                    onDismiss()
                }, Modifier.fillMaxWidth(), color = D.c.hp, soft = D.c.hpSoft)
            }
        },
    )
}

/** Tasks finished per day, from the history: what the journal compares moods against. */
@Composable
private fun rememberDoneByDay(): Map<Long, Int> {
    val c = LocalContext.current.container
    val history by remember { c.db.game().observeHistory() }.collectAsStateWithLifecycle(initialValue = emptyList())
    return remember(history) { history.filter { it.kind != LogKind.VICTORY }.groupingBy { it.day }.eachCount() }
}

/** The hero screen's journal: a month of moods, the average, and how mood and done tasks go together. */
@Composable
fun JournalSection(onOpen: () -> Unit) {
    val c = LocalContext.current.container
    val entries by remember { c.db.game().observeJournal() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val done = rememberDoneByDay()
    var today by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { today = c.game.today() }
    SectionTitle(stringResource(R.string.journal))
    DCard(onClick = onOpen) {
        if (entries.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelImage(Badges.mood(4), pixel = 3.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.journal_empty), style = DType.Muted, modifier = Modifier.weight(1f))
            }
            return@DCard
        }
        val byDay = entries.associateBy { it.day }
        val empty = D.c.line
        // Thirty days of moods, oldest on the left.
        Canvas(Modifier.fillMaxWidth().aspectRatio(15f / 2f)) {
            val cell = size.width / 15f
            val gap = cell * 0.18f
            for (k in 0 until 30) {
                val day = today - 29 + k
                val m = byDay[day]?.mood
                val color = if (m == null) empty else Color(Badges.moodColors[m - 1])
                drawRoundRect(color, Offset((k % 15) * cell + gap / 2, (k / 15) * cell + gap / 2), Size(cell - gap, cell - gap), CornerRadius(cell * 0.14f))
            }
        }
        val month = entries.filter { it.day > today - 30 }
        if (month.isNotEmpty()) {
            Gap(8)
            val avg = month.map { it.mood }.average()
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelImage(Badges.mood(kotlin.math.round(avg).toInt()), pixel = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.journal_avg, moodLabel(kotlin.math.round(avg).toInt()).lowercase()), style = DType.Sub)
            }
        }
        val good = entries.filter { it.mood >= 4 }.map { done[it.day] ?: 0 }
        val hard = entries.filter { it.mood <= 2 }.map { done[it.day] ?: 0 }
        if (good.isNotEmpty() && hard.isNotEmpty()) {
            Gap(6)
            fun f(v: Double) = String.format(Locale.US, "%.1f", v)
            Text(stringResource(R.string.journal_corr, f(good.average()), f(hard.average())), style = DType.Muted)
        }
        Gap(10)
        SoftButton(stringResource(R.string.journal_open), onOpen, Modifier.fillMaxWidth())
    }
}

@Composable
fun JournalScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val entries by remember { c.db.game().observeJournal() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val done = rememberDoneByDay()
    var open by remember { mutableStateOf<JournalEntry?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.journal), onBack)
        if (entries.isEmpty()) Text(stringResource(R.string.journal_empty), style = DType.Sub, modifier = Modifier.padding(24.dp))
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            items(entries, key = { it.day }) { e ->
                Row(
                    Modifier.padding(vertical = 5.dp).fillMaxWidth().clip(D.RMd).background(D.c.surface).border(1.dp, D.c.line, D.RMd)
                        .clickable { open = e }.padding(12.dp),
                ) {
                    PixelImage(Badges.mood(e.mood), pixel = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(dayLabel(e.day), style = DType.Strong, modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.journal_tasks, done[e.day] ?: 0), style = DType.Small)
                        }
                        Text(moodLabel(e.mood), style = DType.Small)
                        if (e.note.isNotBlank()) {
                            Gap(4)
                            Text(e.note, style = DType.Body.copy(fontSize = 14.sp))
                        }
                    }
                }
            }
        }
    }
    open?.let { EntryDialog(it) { open = null } }
}
