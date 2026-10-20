package app.dayquest.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.AppContainer
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Hero
import app.dayquest.game.Catalog
import app.dayquest.game.Outcome
import app.dayquest.game.Rules
import app.dayquest.notify.Reminders
import app.dayquest.pixel.SceneState
import app.dayquest.ui.components.ConfirmDialog
import app.dayquest.ui.components.Gap
import app.dayquest.ui.components.PrimaryButton
import app.dayquest.ui.components.SoftButton
import app.dayquest.ui.components.TopBar
import app.dayquest.ui.look
import app.dayquest.ui.pixel.PixelBar
import app.dayquest.ui.pixel.SceneView
import app.dayquest.ui.rememberSeason
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/** Starting, giving up and collecting focus sessions. The session lives in settings, so it survives the app closing. */
object Focus {
    private val claiming = Mutex()

    suspend fun start(c: AppContainer, context: android.content.Context, minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        c.settings.setFocus(end, minutes)
        Reminders.scheduleFocus(context, end)
    }

    suspend fun giveUp(c: AppContainer, context: android.content.Context) {
        c.settings.setFocus(0, 0)
        Reminders.cancelFocus(context)
    }

    /** Pays a finished session exactly once; returns its minutes and what it gave, or null. */
    suspend fun claim(c: AppContainer, context: android.content.Context): Pair<Int, Outcome>? = claiming.withLock {
        val s = c.settings.now()
        if (s.focusEnd <= 0 || System.currentTimeMillis() < s.focusEnd) return null
        c.settings.setFocus(0, 0)
        Reminders.cancelFocus(context)
        val o = c.game.focusDone(s.focusMinutes) ?: return null
        s.focusMinutes to o
    }
}

@Composable
fun FocusScreen(hero: Hero, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    var minutes by rememberSaveable { mutableIntStateOf(Rules.FOCUS_LENGTHS[1]) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var giveUp by remember { mutableStateOf(false) }
    val running = s.focusEnd > 0
    LaunchedEffect(running) {
        while (running) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    // Keep the screen on while the countdown is showing.
    val view = LocalView.current
    androidx.compose.runtime.DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }
    val pets by remember { c.db.game().observePets() }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.focus), onBack)
        SceneView(app.dayquest.ui.pets.heroScene(hero.copy(resting = false), pets))
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (running) {
                val total = s.focusMinutes * 60_000L
                val left = (s.focusEnd - now).coerceAtLeast(0)
                val sec = (left + 999) / 1000
                Text(stringResource(R.string.focus_running), style = DType.Sub)
                Gap(8)
                Text(String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60), style = DType.Pixel.copy(fontSize = 72.sp, color = D.c.ink))
                Gap(12)
                PixelBar(1f - left.toFloat() / total, D.c.xp)
                Gap(24)
                SoftButton(stringResource(R.string.focus_give_up), { giveUp = true }, Modifier.fillMaxWidth(), color = D.c.hp, soft = D.c.hpSoft)
            } else {
                Text(stringResource(R.string.focus_text), style = DType.Sub, textAlign = TextAlign.Center)
                Gap(20)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Rules.FOCUS_LENGTHS.forEach { m ->
                        val on = m == minutes
                        Box(
                            Modifier.weight(1f).clip(D.RMd).background(if (on) D.c.accentSoft else D.c.surface)
                                .border(if (on) 2.dp else 1.dp, if (on) D.c.accent else D.c.line, D.RMd)
                                .clickable { minutes = m }.padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(R.string.focus_min, m), style = DType.Heading.copy(color = if (on) D.c.accent else D.c.ink)) }
                    }
                }
                Gap(20)
                PrimaryButton(stringResource(R.string.focus_start), { c.launch { Focus.start(c, context, minutes) } }, Modifier.fillMaxWidth())
            }
        }
    }
    if (giveUp) ConfirmDialog(
        stringResource(R.string.focus_give_up_q), stringResource(R.string.focus_give_up_text), stringResource(R.string.focus_give_up),
        onConfirm = { c.launch { Focus.giveUp(c, context) } }, onDismiss = { giveUp = false }, danger = true,
    )
}
