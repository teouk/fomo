package io.github.teouk.fomo.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Straight-line distances on the Earth's surface (the haversine formula). */
object Distance {
    private const val EARTH_RADIUS_MILES = 3958.8

    fun miles(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
        val dLat = Math.toRadians(toLat - fromLat)
        val dLon = Math.toRadians(toLon - fromLon)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_MILES * asin(sqrt(a))
    }
}
