package ma.bosaj.hijri.engine

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionFeaturesTest {
    @Test
    fun calculatesMoroccanHabousAndMwlSolarPrayers() {
        // Rabat, Morocco (34.0209° N, -6.8416° W)
        val day = LocalDate.of(2026, 3, 21)
        val habous = SolarPrayers.calculate(
            date = day,
            latitude = 34.0209,
            longitude = -6.8416,
            utcOffsetHours = 1.0,
            convention = PrayerConvention.MOROCCAN_HABOUS,
            madhab = AsrMadhab.STANDARD,
        )
        assertTrue(habous.fajr.isBefore(habous.shuruq))
        assertTrue(habous.shuruq.isBefore(habous.dhuhr))
        assertTrue(habous.dhuhr.isBefore(habous.asr))
        assertTrue(habous.asr.isBefore(habous.maghrib))
        assertTrue(habous.maghrib.isBefore(habous.isha))

        val hanafi = SolarPrayers.calculate(
            date = day,
            latitude = 34.0209,
            longitude = -6.8416,
            utcOffsetHours = 1.0,
            convention = PrayerConvention.MWL,
            madhab = AsrMadhab.HANAFI,
        )
        assertTrue("Hanafi Asr occurs after Standard Asr", hanafi.asr.isAfter(habous.asr))
    }

    @Test
    fun calculatesGreatCircleQiblaBearingAndRendersSvgCompass() {
        // Casablanca (33.5731° N, -7.5898° W) -> Qibla is roughly ~93.9° East
        val qibla = QiblaCompass.calculate(33.5731, -7.5898)
        assertTrue("Casablanca Qibla bearing ${qibla.bearingDegrees}", qibla.bearingDegrees in 92.0..96.0)
        assertEquals("E", qibla.cardinalDirection)
        assertTrue(qibla.distanceKm in 4600.0..4900.0)

        val svg = QiblaCompass.renderSvgCompass(qibla.bearingDegrees, headingDegrees = 10.0)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("Qibla Compass"))
    }

    @Test
    fun highlightsWhiteDaysAndSunnahFastingWhileSkippingEid() {
        val month = FastingCalendar.forHijriMonth(LocalDate.of(2026, 10, 29), Method.UMM_AL_QURA, -1)
        val whiteDays = month.filter { it.kinds.contains(FastingKind.WHITE_DAY) }
        assertEquals(3, whiteDays.size)
        assertTrue(whiteDays.all { it.isFastingRecommended })

        // 1 Shawwal is Eid al-Fitr: fasting is forbidden
        val ramadanDay = HijriEngine.next(10, 1, LocalDate.of(2026, 1, 1), Method.UMM_AL_QURA)!!
        val eidHighlight = FastingCalendar.evaluateDay(ramadanDay, Method.UMM_AL_QURA)
        assertTrue(eidHighlight.kinds.contains(FastingKind.FORBIDDEN_EID_OR_TASHRIQ))
        assertFalse(eidHighlight.isFastingRecommended)
    }

    @Test
    fun generatesValidIcsFeedForRamadanAndWhiteDays() {
        val start = HijriEngine.next(9, 1, LocalDate.of(2026, 1, 1), Method.UMM_AL_QURA)!!
        val ics = HijriIcsGenerator.generateCalendar(start, daysCount = 45, method = Method.UMM_AL_QURA)
        assertTrue(ics.startsWith("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:Ramadan 1,"))
        assertTrue(ics.contains("Eid al-Fitr"))
        assertTrue(ics.trim().endsWith("END:VCALENDAR"))
    }
}
