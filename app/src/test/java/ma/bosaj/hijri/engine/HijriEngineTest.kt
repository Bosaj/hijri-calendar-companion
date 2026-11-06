package ma.bosaj.hijri.engine

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriEngineTest {
    // tools/hijri_vectors.mjs: 407 days from 1990 to 2076, converted by ICU (Node's Intl).
    private val vectors: JSONObject =
        JSONObject(javaClass.classLoader!!.getResource("icu_vectors.json")!!.readText())

    private fun check(calendar: String, method: Method) {
        val rows: JSONArray = vectors.getJSONArray(calendar)
        assertTrue(rows.length() > 400)
        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            val date = LocalDate.parse(row.getString(0))
            val expected = HijriDate(row.getInt(1), row.getInt(2), row.getInt(3))
            assertEquals("$calendar $date", expected, HijriEngine.toHijri(date, method))
        }
    }

    @Test
    fun ummAlQuraMatchesIcu() = check("umalqura", Method.UMM_AL_QURA)

    @Test
    fun civilMatchesIcu() = check("civil", Method.CIVIL)

    @Test
    fun theOffsetShiftsTheHijriDateByWholeDays() {
        val day = LocalDate.of(2026, 11, 6)
        assertEquals(HijriEngine.toHijri(day.plusDays(1), Method.UMM_AL_QURA), HijriEngine.toHijri(day, Method.UMM_AL_QURA, 1))
        assertEquals(HijriEngine.toHijri(day.minusDays(2), Method.CIVIL), HijriEngine.toHijri(day, Method.CIVIL, -2))
    }

    @Test
    fun matchesTheMoroccanGazetteWithTheRightOffset() {
        // Bulletin Officiel 7556 is dated 17 Jumada al-Ula 1448, 29 October 2026, and 7555 is dated 14 Jumada al-Ula,
        // 26 October 2026. Umm al-Qura was one day ahead of Morocco that month.
        assertEquals(HijriDate(1448, 5, 17), HijriEngine.toHijri(LocalDate.of(2026, 10, 29), Method.UMM_AL_QURA, -1))
        assertEquals(HijriDate(1448, 5, 14), HijriEngine.toHijri(LocalDate.of(2026, 10, 26), Method.UMM_AL_QURA, -1))
        assertEquals(HijriDate(1448, 5, 18), HijriEngine.toHijri(LocalDate.of(2026, 10, 29), Method.UMM_AL_QURA))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnOffsetBeyondTwoDays() {
        HijriEngine.toHijri(LocalDate.of(2026, 11, 6), Method.CIVIL, 3)
    }

    @Test
    fun fallsBackToCivilOutsideTheUmmAlQuraTables() {
        val early = LocalDate.of(1900, 1, 1)
        assertTrue(HijriEngine.isOutsideUmmAlQura(early))
        assertEquals(HijriEngine.toHijri(early, Method.CIVIL), HijriEngine.toHijri(early, Method.UMM_AL_QURA))
        assertFalse(HijriEngine.isOutsideUmmAlQura(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun aMonthIsTwentyNineOrThirtyConsecutiveDays() {
        var day = LocalDate.of(2025, 1, 1)
        repeat(30) {
            for (method in Method.entries) {
                val month = HijriEngine.monthOf(day, method)
                assertTrue("$method ${month.size}", month.size == 29 || month.size == 30)
                assertEquals(1, HijriEngine.toHijri(month.first(), method).day)
                month.zipWithNext { a, b -> assertEquals(a.plusDays(1), b) }
            }
            day = day.plusDays(29)
        }
    }

    @Test
    fun findsTheNextDayWithAGivenHijriDate() {
        val from = LocalDate.of(2026, 11, 6)
        val ramadan = HijriEngine.next(9, 1, from, Method.UMM_AL_QURA)!!
        assertEquals(HijriDate(1448, 9, 1), HijriEngine.toHijri(ramadan, Method.UMM_AL_QURA))
        assertTrue(ramadan.isAfter(from))
        assertNull(HijriEngine.next(13, 1, from, Method.CIVIL))
    }
}
