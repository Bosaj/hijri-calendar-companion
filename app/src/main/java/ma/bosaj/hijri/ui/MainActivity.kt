package ma.bosaj.hijri.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch
import ma.bosaj.hijri.R
import ma.bosaj.hijri.data.Settings
import ma.bosaj.hijri.data.SettingsRepository
import ma.bosaj.hijri.engine.HijriEngine
import ma.bosaj.hijri.engine.Method
import ma.bosaj.hijri.engine.Occasion
import ma.bosaj.hijri.engine.Occasions
import ma.bosaj.hijri.reminders.Notifier
import ma.bosaj.hijri.reminders.Schedules
import ma.bosaj.hijri.widget.HijriWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository(applicationContext)
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(repository)
                }
            }
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun HomeScreen(repository: SettingsRepository) {
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val update: ((Settings) -> Settings) -> Unit = { change ->
        scope.launch {
            repository.update(change)
            val saved = repository.current()
            Schedules.apply(context, saved)
            HijriWidget.refresh(context)
        }
    }
    val today = remember { LocalDate.now() }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TodayCard(today, current)
        MonthCard(today, current)
        UpcomingCard(today, current)
        SettingsCard(current, update)
    }
}

@Composable
private fun TodayCard(today: LocalDate, settings: Settings) {
    val context = LocalContext.current
    val hijri = HijriEngine.toHijri(today, settings.method, settings.offset)
    val occasions = Occasions.on(hijri, today.dayOfWeek)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.today), style = MaterialTheme.typography.labelLarge)
            Text(
                Labels.hijri(context, hijri),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(Labels.gregorian(today), style = MaterialTheme.typography.bodyLarge)
            if (occasions.isNotEmpty()) {
                Text(
                    occasions.joinToString(" · ") { Labels.occasion(context, it) },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (settings.method == Method.UMM_AL_QURA && HijriEngine.isOutsideUmmAlQura(today, settings.offset)) {
                Text(stringResource(R.string.outside_tables), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MonthCard(today: LocalDate, settings: Settings) {
    val context = LocalContext.current
    var anchor by remember { mutableStateOf(today) }
    val days = HijriEngine.monthOf(anchor, settings.method, settings.offset)
    val first = HijriEngine.toHijri(days.first(), settings.method, settings.offset)
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val leading = ((days.first().dayOfWeek.value - firstDayOfWeek.value) + 7) % 7

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { anchor = days.first().minusDays(1) }) { Text(stringResource(R.string.previous)) }
                Text(
                    "${Labels.monthName(context, first.month)} ${first.year}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { anchor = days.last().plusDays(1) }) { Text(stringResource(R.string.next)) }
            }
            Row {
                for (i in 0 until 7) {
                    val dow = firstDayOfWeek.plus(i.toLong())
                    Text(
                        dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            val cells: List<LocalDate?> = List(leading) { null } + days
            for (week in cells.chunked(7)) {
                Row {
                    for (i in 0 until 7) {
                        val day = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (day != null) DayCell(day, today, settings)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer))
                Text(
                    "  " + stringResource(R.string.legend_white_days),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, today: LocalDate, settings: Settings) {
    val context = LocalContext.current
    val hijri = HijriEngine.toHijri(day, settings.method, settings.offset)
    val occasions = Occasions.on(hijri, day.dayOfWeek)
    val background = when {
        day == today -> MaterialTheme.colorScheme.primary
        Occasion.WHITE_DAY in occasions -> MaterialTheme.colorScheme.tertiaryContainer
        occasions.any { it != Occasion.MONDAY_THURSDAY } -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val foreground = if (day == today) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val description = buildString {
        append(Labels.hijri(context, hijri)).append(", ").append(Labels.gregorian(day))
        occasions.forEach { append(", ").append(Labels.occasion(context, it)) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${hijri.day}", color = foreground, style = MaterialTheme.typography.titleSmall)
        Text("${day.dayOfMonth}", color = foreground.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun UpcomingCard(today: LocalDate, settings: Settings) {
    val context = LocalContext.current
    val upcoming = listOf(
        Occasion.RAMADAN_START to (9 to 1),
        Occasion.EID_AL_FITR to (10 to 1),
        Occasion.DAY_OF_ARAFAH to (12 to 9),
        Occasion.EID_AL_ADHA to (12 to 10),
        Occasion.ISLAMIC_NEW_YEAR to (1 to 1),
        Occasion.ASHURA to (1 to 10),
        Occasion.MAWLID to (3 to 12),
    ).mapNotNull { (occasion, date) ->
        HijriEngine.next(date.first, date.second, today, settings.method, settings.offset)?.let { occasion to it }
    }.sortedBy { it.second }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.upcoming), style = MaterialTheme.typography.titleMedium)
            for ((occasion, day) in upcoming) {
                val inDays = ChronoUnit.DAYS.between(today, day)
                Row {
                    Text(Labels.occasion(context, occasion), modifier = Modifier.weight(1f))
                    Text(
                        if (inDays == 0L) stringResource(R.string.today) else pluralStringResource(R.plurals.in_days, inDays.toInt(), inDays.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(Labels.gregorian(day), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.upcoming_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsCard(settings: Settings, update: ((Settings) -> Settings) -> Unit) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(Notifier.canNotify(context)) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
    }
    val anyReminder = settings.remindWhiteDays || settings.remindMondayThursday || settings.remindOccasions

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleMedium)

            Text(stringResource(R.string.method), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Method.entries.forEachIndexed { index, method ->
                    SegmentedButton(
                        selected = settings.method == method,
                        onClick = { update { it.copy(method = method) } },
                        shape = SegmentedButtonDefaults.itemShape(index, Method.entries.size),
                    ) {
                        Text(stringResource(if (method == Method.UMM_AL_QURA) R.string.method_umm_al_qura else R.string.method_civil))
                    }
                }
            }

            Text(stringResource(R.string.offset), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { update { it.copy(offset = it.offset - 1) } },
                    enabled = settings.offset > -HijriEngine.MAX_OFFSET,
                ) { Text("−1") }
                Text(
                    pluralStringResource(R.plurals.offset_days, kotlin.math.abs(settings.offset), signed(settings.offset)),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                    onClick = { update { it.copy(offset = it.offset + 1) } },
                    enabled = settings.offset < HijriEngine.MAX_OFFSET,
                ) { Text("+1") }
            }
            Text(stringResource(R.string.offset_help), style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.reminders), style = MaterialTheme.typography.labelLarge)
            ToggleRow(stringResource(R.string.remind_white_days), settings.remindWhiteDays) { on -> update { it.copy(remindWhiteDays = on) } }
            ToggleRow(stringResource(R.string.remind_monday_thursday), settings.remindMondayThursday) { on -> update { it.copy(remindMondayThursday = on) } }
            ToggleRow(stringResource(R.string.remind_occasions), settings.remindOccasions) { on -> update { it.copy(remindOccasions = on) } }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.reminder_time), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { update { it.copy(reminderHour = (it.reminderHour + 23) % 24) } }) { Text("−") }
                Text(String.format(Locale.getDefault(), "%02d:00", settings.reminderHour), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { update { it.copy(reminderHour = (it.reminderHour + 1) % 24) } }) { Text("+") }
            }
            if (anyReminder && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(stringResource(R.string.notifications_off), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.allow_notifications))
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else "$value"
