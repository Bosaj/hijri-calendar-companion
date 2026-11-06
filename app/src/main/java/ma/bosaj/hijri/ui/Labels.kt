package ma.bosaj.hijri.ui

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import ma.bosaj.hijri.R
import ma.bosaj.hijri.engine.HijriDate
import ma.bosaj.hijri.engine.Occasion

/** Text for dates and occasions, in the app's language. */
object Labels {
    fun monthName(context: Context, month: Int): String =
        context.resources.getStringArray(R.array.hijri_months)[month - 1]

    fun hijri(context: Context, date: HijriDate): String =
        context.getString(R.string.hijri_date_format, date.day, monthName(context, date.month), date.year)

    fun gregorian(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))

    fun occasion(context: Context, occasion: Occasion): String = context.getString(
        when (occasion) {
            Occasion.WHITE_DAY -> R.string.occasion_white_day
            Occasion.MONDAY_THURSDAY -> R.string.occasion_monday_thursday
            Occasion.ISLAMIC_NEW_YEAR -> R.string.occasion_new_year
            Occasion.ASHURA -> R.string.occasion_ashura
            Occasion.MAWLID -> R.string.occasion_mawlid
            Occasion.RAMADAN_START -> R.string.occasion_ramadan
            Occasion.EID_AL_FITR -> R.string.occasion_eid_fitr
            Occasion.DAY_OF_ARAFAH -> R.string.occasion_arafah
            Occasion.EID_AL_ADHA -> R.string.occasion_eid_adha
        },
    )
}
