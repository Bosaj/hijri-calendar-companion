package ma.bosaj.hijri.engine

import java.time.DayOfWeek
import java.time.LocalDate

enum class FastingKind {
    OBLIGATORY_RAMADAN,
    WHITE_DAY,
    MONDAY_THURSDAY,
    ARAFAH,
    ASHURA_TASUA,
    SHAWWAL_SIX,
    FORBIDDEN_EID_OR_TASHRIQ,
}

data class FastingDayHighlight(
    val gregorian: LocalDate,
    val hijri: HijriDate,
    val kinds: List<FastingKind>,
) {
    val isFastingRecommended: Boolean
        get() = kinds.isNotEmpty() && !kinds.contains(FastingKind.FORBIDDEN_EID_OR_TASHRIQ)
}

/**
 * Generates visual monthly fasting calendars highlighting White Days (Ayyam al-Bid: 13, 14, 15),
 * Mondays and Thursdays, Arafah, Ashura/Tasu'a, and Six Days of Shawwal (#10).
 */
object FastingCalendar {
    fun evaluateDay(gregorian: LocalDate, method: Method, offsetDays: Int = 0): FastingDayHighlight {
        val hijri = HijriEngine.toHijri(gregorian, method, offsetDays)
        val (_, month, day) = hijri
        val weekday = gregorian.dayOfWeek
        val kinds = mutableListOf<FastingKind>()

        if ((month == 10 && day == 1) || (month == 12 && day in 10..13)) {
            kinds += FastingKind.FORBIDDEN_EID_OR_TASHRIQ
            return FastingDayHighlight(gregorian, hijri, kinds)
        }

        if (month == 9) {
            kinds += FastingKind.OBLIGATORY_RAMADAN
            return FastingDayHighlight(gregorian, hijri, kinds)
        }

        if (day in 13..15) {
            kinds += FastingKind.WHITE_DAY
        }
        if (weekday == DayOfWeek.MONDAY || weekday == DayOfWeek.THURSDAY) {
            kinds += FastingKind.MONDAY_THURSDAY
        }
        if (month == 12 && day == 9) {
            kinds += FastingKind.ARAFAH
        }
        if (month == 1 && (day == 9 || day == 10)) {
            kinds += FastingKind.ASHURA_TASUA
        }
        if (month == 10 && day in 2..7) {
            kinds += FastingKind.SHAWWAL_SIX
        }

        return FastingDayHighlight(gregorian, hijri, kinds)
    }

    /** Returns all highlighted days in the Hijri month containing [anyDay]. */
    fun forHijriMonth(anyDay: LocalDate, method: Method, offsetDays: Int = 0): List<FastingDayHighlight> =
        HijriEngine.monthOf(anyDay, method, offsetDays).map { evaluateDay(it, method, offsetDays) }
}
