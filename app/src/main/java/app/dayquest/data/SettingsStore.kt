package app.dayquest.data

import android.content.Context
import app.dayquest.game.Challenge
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Hour (0..6) when a new day starts, for night owls. */
    val dayStart: Int = 0,
    val reminders: Boolean = true,
    /** Minutes after midnight for the "dailies left" nudge, -1 when off. */
    val evening: Int = 20 * 60,
    val haptics: Boolean = true,
    /** Seasons flip for the southern hemisphere. */
    val southern: Boolean = false,
    /** 8-bit sound effects (silent anyway when the phone is on silent). */
    val sounds: Boolean = true,
    /** How hard missed tasks hit, picked at the start. */
    val challenge: Challenge = Challenge.NORMAL,
    /** First-visit tips already dismissed. */
    val tips: Set<String> = emptySet(),
    /** A running focus session: when it ends (epoch ms, 0 when none) and how long it is. */
    val focusEnd: Long = 0,
    val focusMinutes: Int = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map {
        Settings(
            theme = it[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dayStart = it[DAY_START] ?: 0,
            reminders = it[REMINDERS] ?: true,
            evening = it[EVENING] ?: (20 * 60),
            haptics = it[HAPTICS] ?: true,
            southern = it[SOUTHERN] ?: false,
            sounds = it[SOUNDS] ?: true,
            challenge = it[CHALLENGE]?.let { c -> runCatching { Challenge.valueOf(c) }.getOrNull() } ?: Challenge.NORMAL,
            tips = it[TIPS] ?: emptySet(),
            focusEnd = it[FOCUS_END] ?: 0,
            focusMinutes = it[FOCUS_MINUTES] ?: 0,
        )
    }

    suspend fun now(): Settings = settings.first()

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDayStart(hour: Int) = store.edit { it[DAY_START] = hour }
    suspend fun setReminders(on: Boolean) = store.edit { it[REMINDERS] = on }
    suspend fun setEvening(minutes: Int) = store.edit { it[EVENING] = minutes }
    suspend fun setHaptics(on: Boolean) = store.edit { it[HAPTICS] = on }
    suspend fun setSouthern(on: Boolean) = store.edit { it[SOUTHERN] = on }
    suspend fun setSounds(on: Boolean) = store.edit { it[SOUNDS] = on }
    suspend fun setChallenge(c: Challenge) = store.edit { it[CHALLENGE] = c.name }
    suspend fun seeTip(key: String) = store.edit { it[TIPS] = (it[TIPS] ?: emptySet()) + key }

    suspend fun setFocus(end: Long, minutes: Int) = store.edit {
        it[FOCUS_END] = end
        it[FOCUS_MINUTES] = minutes
    }

    suspend fun restore(s: Settings) = store.edit {
        it[THEME] = s.theme.name
        it[DAY_START] = s.dayStart
        it[REMINDERS] = s.reminders
        it[EVENING] = s.evening
        it[HAPTICS] = s.haptics
        it[SOUTHERN] = s.southern
        it[SOUNDS] = s.sounds
        it[CHALLENGE] = s.challenge.name
        it[TIPS] = s.tips
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DAY_START = intPreferencesKey("day_start")
        val REMINDERS = booleanPreferencesKey("reminders")
        val EVENING = intPreferencesKey("evening")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SOUTHERN = booleanPreferencesKey("southern")
        val SOUNDS = booleanPreferencesKey("sounds")
        val CHALLENGE = stringPreferencesKey("challenge")
        val TIPS = stringSetPreferencesKey("tips")
        val FOCUS_END = longPreferencesKey("focus_end")
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
    }
}
