package ma.bosaj.hijri.engine

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** Astronomical calculation conventions for daily solar prayer schedules (#8). */
enum class PrayerConvention(
    val fajrAngleDeg: Double,
    val ishaAngleDeg: Double,
    val dhuhrPrecautionMinutes: Int,
    val maghribPrecautionMinutes: Int,
) {
    /** Moroccan Ministry of Habous and Islamic Affairs (19° Fajr, 17° Isha, +5 min Dhuhr/Maghrib precaution). */
    MOROCCAN_HABOUS(19.0, 17.0, 5, 4),

    /** Muslim World League (18° Fajr, 17° Isha). */
    MWL(18.0, 17.0, 1, 1),
}

/** Juristic shadow factor for Asr prayer time. */
enum class AsrMadhab(val shadowFactor: Double) {
    /** Maliki, Shafi'i, and Hanbali (shadow length = 1x object height + noon shadow). */
    STANDARD(1.0),

    /** Hanafi (shadow length = 2x object height + noon shadow). */
    HANAFI(2.0),
}

data class DailyPrayerSchedule(
    val date: LocalDate,
    val convention: PrayerConvention,
    val madhab: AsrMadhab,
    val fajr: LocalTime,
    val shuruq: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
)

object SolarPrayers {
    private const val SUNRISE_SET_ALTITUDE_DEG = -0.8333

    /**
     * Computes Fajr, Shuruq, Dhuhr, Asr, Maghrib, and Isha for a given date, GPS coordinate,
     * and UTC offset (in hours, e.g. +1.0 for Morocco UTC+1).
     */
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        utcOffsetHours: Double = 1.0,
        convention: PrayerConvention = PrayerConvention.MOROCCAN_HABOUS,
        madhab: AsrMadhab = AsrMadhab.STANDARD,
    ): DailyPrayerSchedule {
        require(latitude in -65.0..65.0) { "latitude must be within ±65 degrees" }
        require(longitude in -180.0..180.0) { "longitude must be within ±180 degrees" }

        val jd = julianDay(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)
        val (declination, eqTimeHours) = sunPosition(jd)

        val transit = 12.0 + utcOffsetHours - (longitude / 15.0) - eqTimeHours
        val shuruqDelta = hourAngleHours(latitude, declination, SUNRISE_SET_ALTITUDE_DEG)
        val fajrDelta = hourAngleHours(latitude, declination, -convention.fajrAngleDeg)
        val ishaDelta = hourAngleHours(latitude, declination, -convention.ishaAngleDeg)
        val asrDelta = asrHourAngleHours(latitude, declination, madhab.shadowFactor)

        val fajrHours = transit - fajrDelta
        val shuruqHours = transit - shuruqDelta
        val dhuhrHours = transit + (convention.dhuhrPrecautionMinutes / 60.0)
        val asrHours = transit + asrDelta
        val maghribHours = transit + shuruqDelta + (convention.maghribPrecautionMinutes / 60.0)
        val ishaHours = transit + ishaDelta

        return DailyPrayerSchedule(
            date = date,
            convention = convention,
            madhab = madhab,
            fajr = toLocalTime(fajrHours),
            shuruq = toLocalTime(shuruqHours),
            dhuhr = toLocalTime(dhuhrHours),
            asr = toLocalTime(asrHours),
            maghrib = toLocalTime(maghribHours),
            isha = toLocalTime(ishaHours),
        )
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = normalizeDegrees(357.529 + 0.98560028 * d)
        val q = normalizeDegrees(280.459 + 0.98564736 * d)
        val l = normalizeDegrees(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        val decl = dasin(dsin(e) * dsin(l))
        var ra = Math.toDegrees(kotlin.math.atan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        ra = (ra + 24.0) % 24.0
        var eqTime = q / 15.0 - ra
        if (eqTime > 12.0) eqTime -= 24.0
        if (eqTime < -12.0) eqTime += 24.0
        return decl to eqTime
    }

    private fun hourAngleHours(lat: Double, decl: Double, altitudeDeg: Double): Double {
        val num = dsin(altitudeDeg) - dsin(lat) * dsin(decl)
        val den = dcos(lat) * dcos(decl)
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        return dacos(ratio) / 15.0
    }

    private fun asrHourAngleHours(lat: Double, decl: Double, shadowFactor: Double): Double {
        val angleRad = atan(1.0 / (shadowFactor + tan(Math.toRadians(kotlin.math.abs(lat - decl)))))
        val altitudeDeg = Math.toDegrees(angleRad)
        return hourAngleHours(lat, decl, altitudeDeg)
    }

    private fun toLocalTime(hours: Double): LocalTime {
        val wrapped = ((hours % 24.0) + 24.0) % 24.0
        val totalMinutes = (wrapped * 60.0).roundToInt() % (24 * 60)
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60)
    }

    private fun normalizeDegrees(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0
    private fun dsin(deg: Double): Double = sin(Math.toRadians(deg))
    private fun dcos(deg: Double): Double = cos(Math.toRadians(deg))
    private fun dasin(x: Double): Double = Math.toDegrees(asin(x))
    private fun dacos(x: Double): Double = Math.toDegrees(acos(x))
}
