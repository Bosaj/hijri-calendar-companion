package ma.bosaj.hijri.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ma.bosaj.hijri.engine.HijriEngine
import ma.bosaj.hijri.engine.Method

data class Settings(
    val method: Method = Method.UMM_AL_QURA,
    /** Days added to the calculated date so it matches the local announcement. */
    val offset: Int = 0,
    val remindWhiteDays: Boolean = true,
    val remindMondayThursday: Boolean = false,
    val remindOccasions: Boolean = true,
    /** Hour of the evening before, when reminders for the next day are sent. */
    val reminderHour: Int = 20,
)

private val Context.store by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val method = stringPreferencesKey("method")
        val offset = intPreferencesKey("offset")
        val whiteDays = booleanPreferencesKey("remind_white_days")
        val mondayThursday = booleanPreferencesKey("remind_monday_thursday")
        val occasions = booleanPreferencesKey("remind_occasions")
        val reminderHour = intPreferencesKey("reminder_hour")
    }

    val settings: Flow<Settings> = context.store.data.map(::read)

    suspend fun current(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        context.store.edit { prefs ->
            val next = change(read(prefs))
            prefs[Keys.method] = next.method.name
            prefs[Keys.offset] = next.offset.coerceIn(-HijriEngine.MAX_OFFSET, HijriEngine.MAX_OFFSET)
            prefs[Keys.whiteDays] = next.remindWhiteDays
            prefs[Keys.mondayThursday] = next.remindMondayThursday
            prefs[Keys.occasions] = next.remindOccasions
            prefs[Keys.reminderHour] = next.reminderHour.coerceIn(0, 23)
        }
    }

    private fun read(prefs: Preferences): Settings {
        val defaults = Settings()
        return Settings(
            method = prefs[Keys.method]?.let { name -> Method.entries.firstOrNull { it.name == name } } ?: defaults.method,
            offset = (prefs[Keys.offset] ?: defaults.offset).coerceIn(-HijriEngine.MAX_OFFSET, HijriEngine.MAX_OFFSET),
            remindWhiteDays = prefs[Keys.whiteDays] ?: defaults.remindWhiteDays,
            remindMondayThursday = prefs[Keys.mondayThursday] ?: defaults.remindMondayThursday,
            remindOccasions = prefs[Keys.occasions] ?: defaults.remindOccasions,
            reminderHour = prefs[Keys.reminderHour] ?: defaults.reminderHour,
        )
    }
}
