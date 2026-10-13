package app.dayquest.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.LogKind
import app.dayquest.ui.components.DCard
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.SectionTitle
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.flow.emptyFlow

/** The last 30 days at a glance: what goes well, and what slips most. Tapping a task opens its statistics. */
@Composable
fun InsightsSection(onTask: (Long) -> Unit) {
    val c = LocalContext.current.container
    var today by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { today = c.game.today() }
    val events by remember(today) { if (today == 0L) emptyFlow() else c.db.game().observeTaskEventsSince(today - 29) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by remember { c.db.game().observeTasks() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val byId = tasks.associateBy { it.id }
    val grouped = events.filter { it.taskId in byId }.groupBy { it.taskId }
    val best = grouped.mapValues { (_, l) -> l.count { it.kind == LogKind.UP || it.kind == LogKind.DAILY || it.kind == LogKind.TODO } }
        .filterValues { it > 0 }.entries.sortedByDescending { it.value }.take(3)
    val worst = grouped.mapValues { (_, l) -> l.count { it.kind == LogKind.DOWN || it.kind == LogKind.MISSED } }
        .filterValues { it > 0 }.entries.sortedByDescending { it.value }.take(3)
    if (best.isEmpty() && worst.isEmpty()) return
    SectionTitle(stringResource(R.string.insights))
    DCard {
        if (best.isNotEmpty()) {
            Text(stringResource(R.string.insights_best).uppercase(), style = DType.Eyebrow.copy(color = D.c.good))
            best.forEach { (id, n) -> Line(byId[id]?.title.orEmpty(), pluralStringResource(R.plurals.insights_times, n, n)) { onTask(id) } }
        }
        if (best.isNotEmpty() && worst.isNotEmpty()) {
            Gap(6)
            HorizontalDivider(color = D.c.line)
            Gap(8)
        }
        if (worst.isNotEmpty()) {
            Text(stringResource(R.string.insights_worst).uppercase(), style = DType.Eyebrow.copy(color = D.c.hp))
            worst.forEach { (id, n) -> Line(byId[id]?.title.orEmpty(), pluralStringResource(R.plurals.insights_misses, n, n)) { onTask(id) } }
        }
    }
}

@Composable
private fun Line(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(D.RXs).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = DType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(value, style = DType.Number.copy(color = D.c.muted))
    }
}
