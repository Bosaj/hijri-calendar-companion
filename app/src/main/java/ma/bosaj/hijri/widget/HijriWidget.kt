package ma.bosaj.hijri.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.LocalDate
import ma.bosaj.hijri.data.SettingsRepository
import ma.bosaj.hijri.engine.HijriEngine
import ma.bosaj.hijri.engine.Occasions
import ma.bosaj.hijri.ui.Labels
import ma.bosaj.hijri.ui.MainActivity

/** Home-screen widget: the Hijri date above the Gregorian one, and what falls today. */
class HijriWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsRepository(context).current()
        val today = LocalDate.now()
        val hijri = HijriEngine.toHijri(today, settings.method, settings.offset)
        val hijriText = Labels.hijri(context, hijri)
        val gregorianText = Labels.gregorian(today)
        val todayText = Occasions.on(hijri, today.dayOfWeek)
            .joinToString(" · ") { Labels.occasion(context, it) }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(20.dp)
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = hijriText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = gregorianText,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        maxLines = 1,
                    )
                    if (todayText.isNotEmpty()) {
                        Text(
                            text = todayText,
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = HijriWidget().updateAll(context)
    }
}

class HijriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HijriWidget()
}
