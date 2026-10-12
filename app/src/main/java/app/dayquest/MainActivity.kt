package app.dayquest

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import app.dayquest.data.Settings
import app.dayquest.ui.DayquestRoot
import app.dayquest.ui.theme.DayquestTheme
import app.dayquest.ui.theme.isDark

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Screenshot builds take the language from the launch command: `am start ... -e lang ru`.
        if (BuildConfig.DEMO && savedInstanceState == null) {
            val lang = intent?.getStringExtra("lang")
            lang?.let { app.dayquest.ui.Language.set(it) }
            // Seed in the requested language, so the made-up tasks match the screens around them.
            val ctx = lang?.let { l ->
                createConfigurationContext(android.content.res.Configuration(resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(l)) })
            } ?: this
            container.scope.launch { app.dayquest.data.DemoSeed.seedIfEmpty(container, ctx) }
        }
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = isDark((settings ?: Settings()).theme)
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            DayquestTheme(dark) {
                // Wait for settings so the theme doesn't flash light-then-dark.
                settings?.let { DayquestRoot(it) }
            }
        }
    }
}
