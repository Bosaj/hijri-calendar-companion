package ma.bosaj.hijri.engine

import java.time.LocalDate
import kotlin.math.ceil

/** A date in the Hijri calendar. Months run from 1 (Muharram) to 12 (Dhu al-Hijja). */
data class HijriDate(val year: Int, val month: Int, val day: Int)

/**
 * How a Gregorian day becomes a Hijri one. Neither method predicts countries that start each month on a
 * local moon sighting, Morocco among them, which is why every conversion takes a day offset too.
 */
enum class Method {
    /** Saudi Arabia's official calendar, from its published tables (1356–1500 AH). */
    UMM_AL_QURA,

    /** The arithmetic "civil" calendar: a fixed 30-year cycle, 11 leap years, epoch 16 July 622 (Julian). */
    CIVIL,
}

object HijriEngine {
    const val MAX_OFFSET = 2

    /** Julian day 1948440, 1 Muharram 1 AH in the civil calendar, as a java.time epoch day. */
    private const val CIVIL_EPOCH_DAY = -492148L

    fun toHijri(date: LocalDate, method: Method, offsetDays: Int = 0): HijriDate {
        require(offsetDays in -MAX_OFFSET..MAX_OFFSET) { "offset must be within ±$MAX_OFFSET days" }
        val day = date.toEpochDay() + offsetDays
        return when (method) {
            // Outside the published tables, the civil calendar is the honest fallback.
            Method.UMM_AL_QURA -> ummAlQura(day) ?: civil(day)
            Method.CIVIL -> civil(day)
        }
    }

    /** True when Umm al-Qura is asked for a day its tables do not cover, so the civil calendar answered. */
    fun isOutsideUmmAlQura(date: LocalDate, offsetDays: Int = 0): Boolean =
        ummAlQura(date.toEpochDay() + offsetDays) == null

    /** The Gregorian days of the Hijri month that contains [anyDay], in order. */
    fun monthOf(anyDay: LocalDate, method: Method, offsetDays: Int = 0): List<LocalDate> {
        val target = toHijri(anyDay, method, offsetDays)
        var first = anyDay
        while (toHijri(first.minusDays(1), method, offsetDays).month == target.month) first = first.minusDays(1)
        val days = mutableListOf<LocalDate>()
        var day = first
        while (toHijri(day, method, offsetDays).month == target.month) {
            days += day
            day = day.plusDays(1)
        }
        return days
    }

    /** The first Gregorian day on or after [from] whose Hijri date is [month]/[day], within a year and a half. */
    fun next(month: Int, day: Int, from: LocalDate, method: Method, offsetDays: Int = 0): LocalDate? {
        var date = from
        repeat(MAX_SEARCH_DAYS) {
            val hijri = toHijri(date, method, offsetDays)
            if (hijri.month == month && hijri.day == day) return date
            date = date.plusDays(1)
        }
        return null
    }

    private const val MAX_SEARCH_DAYS = 550

    private fun ummAlQura(epochDay: Long): HijriDate? {
        var start = UmmAlQuraData.FIRST_YEAR_START_EPOCH_DAY
        if (epochDay < start) return null
        for ((index, mask) in UmmAlQuraData.MONTH_MASKS.withIndex()) {
            for (month in 0 until 12) {
                val length = if (mask and (1 shl month) != 0) 30 else 29
                if (epochDay < start + length) {
                    return HijriDate(UmmAlQuraData.FIRST_YEAR + index, month + 1, (epochDay - start).toInt() + 1)
                }
                start += length
            }
        }
        return null
    }

    // The same arithmetic as ICU's islamic-civil calendar.
    private fun civil(epochDay: Long): HijriDate {
        val days = epochDay - CIVIL_EPOCH_DAY
        val year = Math.floorDiv(30 * days + 10646, 10631L).toInt()
        val month = minOf(ceil((days - 29 - civilYearStart(year)) / 29.5).toInt(), 11).coerceAtLeast(0)
        val day = (days - civilMonthStart(year, month)).toInt() + 1
        return HijriDate(year, month + 1, day)
    }

    private fun civilYearStart(year: Int): Long = (year - 1) * 354L + Math.floorDiv(3 + 11L * year, 30L)

    private fun civilMonthStart(year: Int, zeroBasedMonth: Int): Long =
        ceil(29.5 * zeroBasedMonth).toLong() + civilYearStart(year)
}
