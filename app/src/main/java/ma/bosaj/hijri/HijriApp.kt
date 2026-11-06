package ma.bosaj.hijri

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ma.bosaj.hijri.data.SettingsRepository
import ma.bosaj.hijri.reminders.Notifier
import ma.bosaj.hijri.reminders.Schedules
import ma.bosaj.hijri.widget.HijriWidget

class HijriApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        scope.launch { Schedules.apply(this@HijriApp, SettingsRepository(this@HijriApp).current()) }
    }
}

/** The day can change without midnight passing: the user sets the clock or crosses a time zone. */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        val app = context.applicationContext as HijriApp
        val pending = goAsync()
        app.scope.launch {
            try {
                Schedules.apply(context, SettingsRepository(context).current())
                HijriWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}
