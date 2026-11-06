package ma.bosaj.hijri.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import ma.bosaj.hijri.R
import ma.bosaj.hijri.data.Settings
import ma.bosaj.hijri.data.SettingsRepository
import ma.bosaj.hijri.engine.HijriEngine
import ma.bosaj.hijri.engine.Occasion
import ma.bosaj.hijri.engine.Occasions
import ma.bosaj.hijri.ui.Labels
import ma.bosaj.hijri.ui.MainActivity
import ma.bosaj.hijri.widget.HijriWidget

/** Which of tomorrow's occasions the user asked to hear about. */
fun remindersFor(day: LocalDate, settings: Settings): List<Occasion> {
    val hijri = HijriEngine.toHijri(day, settings.method, settings.offset)
    return Occasions.on(hijri, day.dayOfWeek).filter { occasion ->
        when (occasion) {
            Occasion.WHITE_DAY -> settings.remindWhiteDays
            Occasion.MONDAY_THURSDAY -> settings.remindMondayThursday
            else -> settings.remindOccasions
        }
    }
}

/** Runs each evening and tells the user what tomorrow brings. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).current()
        val tomorrow = LocalDate.now().plusDays(1)
        val occasions = remindersFor(tomorrow, settings)
        if (occasions.isNotEmpty()) Notifier.show(applicationContext, tomorrow, settings, occasions)
        return Result.success()
    }
}

/** Runs just after midnight, so the widget shows the new day. */
class MidnightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        HijriWidget.refresh(applicationContext)
        return Result.success()
    }
}

object Schedules {
    private const val REMINDER_WORK = "evening-reminder"
    private const val MIDNIGHT_WORK = "midnight-refresh"

    fun apply(context: Context, settings: Settings) {
        val work = WorkManager.getInstance(context)
        val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(LocalTime.of(settings.reminderHour, 0)).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(REMINDER_WORK, ExistingPeriodicWorkPolicy.UPDATE, reminder)
        val midnight = PeriodicWorkRequestBuilder<MidnightWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(LocalTime.of(0, 1)).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(MIDNIGHT_WORK, ExistingPeriodicWorkPolicy.UPDATE, midnight)
    }

    private fun delayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}

object Notifier {
    private const val CHANNEL = "reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminders_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, day: LocalDate, settings: Settings, occasions: List<Occasion>) {
        if (!canNotify(context)) return
        val hijri = HijriEngine.toHijri(day, settings.method, settings.offset)
        val title = context.getString(
            R.string.reminder_title,
            occasions.joinToString(context.getString(R.string.list_separator)) { Labels.occasion(context, it) },
        )
        var text = context.getString(R.string.reminder_text, Labels.hijri(context, hijri))
        if (occasions.any { it.needsSighting }) text += " " + context.getString(R.string.reminder_sighting)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(day.toEpochDay().toInt(), notification)
        } catch (_: SecurityException) {
            // The permission was withdrawn between the check and the call.
        }
    }
}
