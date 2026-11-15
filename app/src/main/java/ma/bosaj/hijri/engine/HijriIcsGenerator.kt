package ma.bosaj.hijri.engine

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generates RFC 5545 iCalendar (.ics) feeds for Ramadan, Tarawih, White Days, and Sunnah fasting (#11).
 */
object HijriIcsGenerator {
    private val ICS_DATE = DateTimeFormatter.BASIC_ISO_DATE

    fun generateCalendar(
        startDate: LocalDate,
        daysCount: Int = 365,
        method: Method = Method.UMM_AL_QURA,
        offsetDays: Int = 0,
        includeWhiteDays: Boolean = true,
        includeRamadanTarawih: Boolean = true,
    ): String {
        require(daysCount in 1..730) { "daysCount must be between 1 and 730" }

        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Bosaj//Hijri Calendar Companion//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")
        sb.appendLine("X-WR-CALNAME:Hijri & Ramadan Companion")

        for (i in 0 until daysCount) {
            val day = startDate.plusDays(i.toLong())
            val hijri = HijriEngine.toHijri(day, method, offsetDays)
            val occasions = Occasions.on(hijri, day.dayOfWeek)

            if (includeRamadanTarawih && hijri.month == 9) {
                appendAllDayEvent(
                    sb = sb,
                    uid = "ramadan-${hijri.year}-${hijri.day}@hijri.bosaj.ma",
                    date = day,
                    summary = "Ramadan ${hijri.day}, ${hijri.year} AH — Fasting & Tarawih",
                    description = "Blessed month of Ramadan (${hijri.day}/9/${hijri.year} AH). Evening Tarawih prayer after Isha.",
                )
            }

            for (occ in occasions) {
                if (occ == Occasion.MONDAY_THURSDAY) continue
                if (occ == Occasion.WHITE_DAY && !includeWhiteDays) continue
                val title = when (occ) {
                    Occasion.WHITE_DAY -> "White Day Fast (${hijri.day}/${hijri.month}/${hijri.year} AH)"
                    Occasion.ISLAMIC_NEW_YEAR -> "Islamic New Year (1 Muharram ${hijri.year} AH)"
                    Occasion.ASHURA -> "Day of Ashura (10 Muharram ${hijri.year} AH)"
                    Occasion.MAWLID -> "Mawlid al-Nabawi (12 Rabi' al-Awwal ${hijri.year} AH)"
                    Occasion.RAMADAN_START -> "First of Ramadan (${hijri.year} AH)"
                    Occasion.EID_AL_FITR -> "Eid al-Fitr (1 Shawwal ${hijri.year} AH)"
                    Occasion.DAY_OF_ARAFAH -> "Day of Arafah (9 Dhu al-Hijja ${hijri.year} AH)"
                    Occasion.EID_AL_ADHA -> "Eid al-Adha (10 Dhu al-Hijja ${hijri.year} AH)"
                    Occasion.MONDAY_THURSDAY -> "Sunnah Fast"
                }
                appendAllDayEvent(
                    sb = sb,
                    uid = "${occ.name.lowercase()}-${day.format(ICS_DATE)}@hijri.bosaj.ma",
                    date = day,
                    summary = title,
                    description = if (occ.needsSighting) "$title — Confirm local moon sighting." else title,
                )
            }
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun appendAllDayEvent(
        sb: StringBuilder,
        uid: String,
        date: LocalDate,
        summary: String,
        description: String,
    ) {
        val dtStart = date.format(ICS_DATE)
        val dtEnd = date.plusDays(1).format(ICS_DATE)
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("UID:$uid")
        sb.appendLine("DTSTAMP:${dtStart}T000000Z")
        sb.appendLine("DTSTART;VALUE=DATE:$dtStart")
        sb.appendLine("DTEND;VALUE=DATE:$dtEnd")
        sb.appendLine("SUMMARY:$summary")
        sb.appendLine("DESCRIPTION:$description")
        sb.appendLine("END:VEVENT")
    }
}
