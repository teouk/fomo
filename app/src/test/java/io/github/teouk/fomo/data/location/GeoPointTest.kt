package io.github.teouk.fomo.data.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoPointTest {
    @Test
    fun isInUk_swansea_isTrue() {
        assertTrue(GeoPoint(51.6214, -3.9436).isInUk)
    }

    @Test
    fun isInUk_emulatorDefaultMountainView_isFalse() {
        // The Android emulator's default position, which caused an empty map.
        assertFalse(GeoPoint(37.4220, -122.0841).isInUk)
    }
}
