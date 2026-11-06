package ma.bosaj.hijri.engine

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccasionsTest {
    @Test
    fun whiteDaysAreTheThirteenthToTheFifteenth() {
        assertEquals(listOf(Occasion.WHITE_DAY), Occasions.on(HijriDate(1448, 8, 13), FRIDAY))
        assertEquals(listOf(Occasion.WHITE_DAY), Occasions.on(HijriDate(1448, 8, 15), FRIDAY))
        assertTrue(Occasions.on(HijriDate(1448, 8, 16), FRIDAY).isEmpty())
    }

    @Test
    fun noFastingReminderInRamadanOnEidOrInTashriq() {
        assertTrue(Occasions.on(HijriDate(1448, 9, 14), MONDAY).isEmpty())
        assertEquals(listOf(Occasion.EID_AL_FITR), Occasions.on(HijriDate(1448, 10, 1), THURSDAY))
        assertEquals(listOf(Occasion.EID_AL_ADHA), Occasions.on(HijriDate(1448, 12, 10), MONDAY))
        assertTrue(Occasions.on(HijriDate(1448, 12, 13), MONDAY).isEmpty())
    }

    @Test
    fun mondayAndThursdayOutsideThoseDays() {
        assertEquals(listOf(Occasion.MONDAY_THURSDAY), Occasions.on(HijriDate(1448, 5, 2), THURSDAY))
        assertEquals(
            listOf(Occasion.DAY_OF_ARAFAH, Occasion.MONDAY_THURSDAY),
            Occasions.on(HijriDate(1448, 12, 9), MONDAY),
        )
    }

    @Test
    fun yearlyOccasions() {
        assertEquals(listOf(Occasion.ISLAMIC_NEW_YEAR), Occasions.on(HijriDate(1449, 1, 1), FRIDAY))
        assertEquals(listOf(Occasion.ASHURA), Occasions.on(HijriDate(1449, 1, 10), FRIDAY))
        assertEquals(listOf(Occasion.MAWLID), Occasions.on(HijriDate(1449, 3, 12), FRIDAY))
        assertEquals(listOf(Occasion.RAMADAN_START), Occasions.on(HijriDate(1448, 9, 1), FRIDAY))
        assertTrue(Occasion.RAMADAN_START.needsSighting)
        assertTrue(!Occasion.WHITE_DAY.needsSighting)
    }
}
