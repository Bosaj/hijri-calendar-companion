package ma.bosaj.hijri.engine

import java.time.DayOfWeek

/** Days the app can remind about. */
enum class Occasion(
    /** True when the date depends on a moon sighting, so the reminder asks to confirm the announcement. */
    val needsSighting: Boolean,
) {
    WHITE_DAY(false),
    MONDAY_THURSDAY(false),
    ISLAMIC_NEW_YEAR(true),
    ASHURA(true),
    MAWLID(true),
    RAMADAN_START(true),
    EID_AL_FITR(true),
    DAY_OF_ARAFAH(true),
    EID_AL_ADHA(true),
}

object Occasions {
    private const val RAMADAN = 9
    private const val SHAWWAL = 10
    private const val DHU_AL_HIJJA = 12

    /** What falls on a day. Fasting reminders skip days when fasting is not done: Ramadan, the Eids and Tashriq. */
    fun on(date: HijriDate, weekday: DayOfWeek): List<Occasion> {
        val found = mutableListOf<Occasion>()
        val (_, month, day) = date
        when {
            month == 1 && day == 1 -> found += Occasion.ISLAMIC_NEW_YEAR
            month == 1 && day == 10 -> found += Occasion.ASHURA
            month == 3 && day == 12 -> found += Occasion.MAWLID
            month == RAMADAN && day == 1 -> found += Occasion.RAMADAN_START
            month == SHAWWAL && day == 1 -> found += Occasion.EID_AL_FITR
            month == DHU_AL_HIJJA && day == 9 -> found += Occasion.DAY_OF_ARAFAH
            month == DHU_AL_HIJJA && day == 10 -> found += Occasion.EID_AL_ADHA
        }
        if (fastingAllowedOutsideRamadan(date)) {
            if (day in 13..15) found += Occasion.WHITE_DAY
            if (weekday == DayOfWeek.MONDAY || weekday == DayOfWeek.THURSDAY) found += Occasion.MONDAY_THURSDAY
        }
        return found
    }

    private fun fastingAllowedOutsideRamadan(date: HijriDate): Boolean {
        val (_, month, day) = date
        if (month == RAMADAN) return false
        if (month == SHAWWAL && day == 1) return false
        if (month == DHU_AL_HIJJA && day in 10..13) return false
        return true
    }
}
