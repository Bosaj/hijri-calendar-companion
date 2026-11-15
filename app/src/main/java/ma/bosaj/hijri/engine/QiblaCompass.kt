package ma.bosaj.hijri.engine

import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class QiblaVector(
    val bearingDegrees: Double,
    val distanceKm: Double,
    val cardinalDirection: String,
)

/**
 * Great Circle initial bearing and Haversine distance calculator to the Kaaba in Makkah
 * (21.422487° N, 39.826206° E), plus interactive SVG compass generator (#9).
 */
object QiblaCompass {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0088

    private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    fun calculate(latitude: Double, longitude: Double): QiblaVector {
        require(latitude in -90.0..90.0) { "latitude must be within ±90 degrees" }
        require(longitude in -180.0..180.0) { "longitude must be within ±180 degrees" }

        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - longitude)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = Math.toDegrees(atan2(y, x))
        val bearing = (theta + 360.0) % 360.0

        val dPhi = phi2 - phi1
        val a = sin(dPhi / 2.0).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2)
        val c = 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        val distanceKm = EARTH_RADIUS_KM * c

        val cardinalIndex = ((bearing / 45.0).roundToInt()) % 8
        return QiblaVector(
            bearingDegrees = Math.round(bearing * 100.0) / 100.0,
            distanceKm = Math.round(distanceKm * 10.0) / 10.0,
            cardinalDirection = CARDINALS[cardinalIndex],
        )
    }

    /** Generates a clean, self-contained interactive SVG Qibla compass rotated to [bearingDegrees]. */
    fun renderSvgCompass(bearingDegrees: Double, headingDegrees: Double = 0.0, sizePx: Int = 240): String {
        val relativeAngle = ((bearingDegrees - headingDegrees) % 360.0 + 360.0) % 360.0
        val formattedAngle = String.format(Locale.US, "%.1f", relativeAngle)
        val formattedBearing = String.format(Locale.US, "%.1f", bearingDegrees)
        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="$sizePx" height="$sizePx" role="img" aria-label="Qibla Compass $formattedBearing°">
              <circle cx="100" cy="100" r="92" fill="#0F172A" stroke="#D4AF37" stroke-width="3"/>
              <text x="100" y="24" text-anchor="middle" fill="#F8FAFC" font-size="12" font-weight="bold">N</text>
              <text x="180" y="104" text-anchor="middle" fill="#94A3B8" font-size="11">E</text>
              <text x="100" y="186" text-anchor="middle" fill="#94A3B8" font-size="11">S</text>
              <text x="20" y="104" text-anchor="middle" fill="#94A3B8" font-size="11">W</text>
              <g transform="rotate($formattedAngle 100 100)">
                <polygon points="100,28 92,100 108,100" fill="#10B981"/>
                <polygon points="100,172 92,100 108,100" fill="#475569"/>
                <rect x="93" y="20" width="14" height="14" rx="2" fill="#1E293B" stroke="#D4AF37" stroke-width="2"/>
              </g>
              <circle cx="100" cy="100" r="5" fill="#D4AF37"/>
            </svg>
        """.trimIndent()
    }
}
