package io.github.teouk.fomo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test names follow: functionUnderTest_condition_expectedResult. */
class GeoHashTest {
    @Test
    fun encode_referenceCoordinate_returnsPublishedHash() {
        // Reference value from the geohash article on Wikipedia.
        assertEquals("u4pruydqq", GeoHash.encode(57.64911, 10.40744))
    }

    @Test
    fun encode_westernHemisphere_returnsPublishedHash() {
        assertEquals("gbsuv", GeoHash.encode(48.669, -4.329, precision = 5))
    }

    @Test
    fun encode_shorterPrecision_isPrefixOfLongerHash() {
        val swanseaLat = 51.6214
        val swanseaLon = -3.9436
        val long = GeoHash.encode(swanseaLat, swanseaLon, precision = 9)
        val short = GeoHash.encode(swanseaLat, swanseaLon, precision = 5)
        assertEquals(5, short.length)
        assertTrue(long.startsWith(short))
    }

    @Test
    fun encode_extremeCorners_doesNotCrash() {
        assertEquals(9, GeoHash.encode(90.0, 180.0).length)
        assertEquals(9, GeoHash.encode(-90.0, -180.0).length)
    }

    @Test
    fun encode_latitudeOutOfRange_throws() {
        assertThrows(IllegalArgumentException::class.java) { GeoHash.encode(91.0, 0.0) }
    }

    @Test
    fun encode_precisionAboveMaximum_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoHash.encode(0.0, 0.0, precision = GeoHash.MAX_PRECISION + 1)
        }
    }
}
