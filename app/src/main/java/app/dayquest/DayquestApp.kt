package app.dayquest

import android.app.Application
import android.content.Context
import app.dayquest.data.GameDatabase
import app.dayquest.data.SettingsStore
import app.dayquest.game.Game
import app.dayquest.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = GameDatabase.create(context)
    val settings = SettingsStore(context)
    val game = Game(db.game(), settings)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun launch(block: suspend () -> Unit) {
        scope.launch {
            block()
            Reminders.reschedule(context)
        }
    }
}

class DayquestApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.ensureChannel(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            Reminders.reschedule(this@DayquestApp)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as DayquestApp).container
