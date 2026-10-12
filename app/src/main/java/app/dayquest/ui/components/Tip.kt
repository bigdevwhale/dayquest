package app.dayquest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.dayquest.BuildConfig
import app.dayquest.R
import app.dayquest.container
import app.dayquest.pixel.Sprites
import app.dayquest.ui.pixel.PixelImage
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType
import kotlinx.coroutines.launch

/** A one-time hint on a screen's first visit; "Got it" hides it for good. */
@Composable
fun Tip(key: String, text: String, modifier: Modifier = Modifier) {
    val c = LocalContext.current.container
    val settings by remember { c.settings.settings }.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    // Screenshot builds stay clean.
    if (BuildConfig.DEMO) return
    var hidden by remember { mutableStateOf(false) }
    AnimatedVisibility(key !in s.tips && !hidden, exit = fadeOut() + shrinkVertically()) {
        Row(
            modifier.fillMaxWidth().clip(D.RMd).background(D.c.accentSoft).padding(start = 14.dp, top = 12.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PixelImage(Sprites.star, pixel = 2.dp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text, style = DType.Body.copy(color = D.c.ink))
                TextButton(onClick = { hidden = true; c.scope.launch { c.settings.seeTip(key) } }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.tip_ok), color = D.c.accent, style = DType.Button)
                }
            }
        }
    }
}
