package io.github.teouk.fomo.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {
    @Test
    fun miles_samePoint_isZero() {
        assertEquals(0.0, Distance.miles(51.62, -3.94, 51.62, -3.94), 0.0001)
    }

    @Test
    fun miles_swanseaToCardiff_isAbout34Miles() {
        // Straight-line distance between the two city centres is about 34 miles.
        assertEquals(34.2, Distance.miles(51.6214, -3.9436, 51.4816, -3.1791), 0.5)
    }
}
