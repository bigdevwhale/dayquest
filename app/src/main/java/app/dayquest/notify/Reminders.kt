package app.dayquest.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import app.dayquest.MainActivity
import app.dayquest.R
import app.dayquest.container
import app.dayquest.data.Task
import app.dayquest.data.TaskType
import app.dayquest.game.Rules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** One alarm at the soonest upcoming reminder; on firing, it reports everything that came due since the last check. */
object Reminders {
    private const val CHANNEL_ID = "reminders_v1"
    const val ACTION_FIRE = "app.dayquest.notify.FIRE"
    const val ACTION_FOCUS = "app.dayquest.notify.FOCUS"
    private const val FOCUS_ID = 2
    private const val PREFS = "reminders"
    private const val LAST_CHECK = "last_check"
    private const val EVENING_ID = 1

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_desc)
            }
        )
    }

    private fun fireIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun focusIntent(context: Context) = PendingIntent.getBroadcast(
        context, 2,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FOCUS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Rings when a focus session ends, even with the app closed (give or take a minute, like reminders). */
    fun scheduleFocus(context: Context, at: Long) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, focusIntent(context))
    }

    fun cancelFocus(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(focusIntent(context))
        NotificationManagerCompat.from(context).cancel(FOCUS_ID)
    }

    fun notifyFocus(context: Context) {
        if (canNotify(context)) notify(context, FOCUS_ID, context.getString(R.string.notif_focus), context.getString(R.string.notif_focus_text))
    }

    private fun nextAt(minutes: Int, after: Long): Long {
        val zone = ZoneId.systemDefault()
        val time = LocalTime.of(minutes / 60, minutes % 60)
        var at = LocalDate.now(zone).atTime(time).atZone(zone).toInstant().toEpochMilli()
        if (at <= after) at = LocalDate.now(zone).plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
        return at
    }

    /** Clock times (today) that fell inside (since, now]. */
    private fun crossed(minutes: Int, since: Long, now: Long): Boolean {
        val zone = ZoneId.systemDefault()
        val time = LocalTime.of(minutes / 60, minutes % 60)
        val today = LocalDate.now(zone)
        return listOf(today, today.minusDays(1)).any {
            val at = it.atTime(time).atZone(zone).toInstant().toEpochMilli()
            at in (since + 1)..now
        }
    }

    suspend fun reschedule(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = prefs(context)
        if (!prefs.contains(LAST_CHECK)) prefs.edit { putLong(LAST_CHECK, now) }
        val settings = context.container.settings.now()
        val times = buildList {
            if (settings.reminders) context.container.db.game().tasks().forEach { if (it.reminder >= 0) add(it.reminder) }
            if (settings.evening >= 0) add(settings.evening)
        }
        val heartbeat = now + 6 * 60 * 60 * 1000L
        val at = (times.map { nextAt(it, now) } + heartbeat).min()
        // Reminders don't need to-the-second precision; this avoids the SCHEDULE_EXACT_ALARM permission.
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fireIntent(context))
    }

    suspend fun fire(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = prefs(context)
        val since = prefs.getLong(LAST_CHECK, now)
        prefs.edit { putLong(LAST_CHECK, now) }
        if (!canNotify(context)) return
        val settings = context.container.settings.now()
        val tasks = context.container.db.game().tasks()
        val today = Rules.logicalDay(LocalDateTime.now(), settings.dayStart)
        fun pending(t: Task) = when (t.type) {
            TaskType.DAILY -> Rules.isDue(t, today) && t.doneDay != today
            TaskType.TODO -> !t.done
            else -> false
        }
        if (settings.reminders) {
            tasks.filter { it.reminder >= 0 && pending(it) && crossed(it.reminder, since, now) }.forEach {
                notify(context, 100 + it.id.toInt(), context.getString(R.string.notif_task, it.title), context.getString(R.string.notif_task_text))
            }
        }
        if (settings.evening >= 0 && crossed(settings.evening, since, now)) {
            val left = tasks.count { it.type == TaskType.DAILY && pending(it) }
            if (left > 0) notify(
                context, EVENING_ID,
                context.resources.getQuantityString(R.plurals.notif_evening, left, left),
                context.getString(R.string.notif_evening_text),
            )
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Reminders.ACTION_FOCUS) {
            Reminders.notifyFocus(context)
            return
        }
        if (intent.action != Reminders.ACTION_FIRE) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Reminders.fire(context)
                Reminders.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Reminders.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
