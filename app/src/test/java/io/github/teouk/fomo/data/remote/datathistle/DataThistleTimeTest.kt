package io.github.teouk.fomo.data.remote.datathistle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DataThistleTimeTest {
    @Test
    fun parse_isoWithOffset_isExact() {
        assertEquals(Instant.parse("2025-04-01T09:30:00Z"), DataThistleTime.parse("2025-04-01T10:30:00+01:00"))
    }

    @Test
    fun parse_spaceAndHourOnlyOffset_isAccepted() {
        assertEquals(Instant.parse("2025-04-10T17:45:00Z"), DataThistleTime.parse("2025-04-10 18:45:00+01"))
    }

    @Test
    fun parse_noOffsetInSummer_treatedAsUkTime() {
        // 30 March 2025 is after the clocks went forward, so UK time is UTC+1.
        assertEquals(Instant.parse("2025-03-30T08:30:00Z"), DataThistleTime.parse("2025-03-30T09:30:00"))
    }

    @Test
    fun parse_blankOrGarbage_returnsNull() {
        assertNull(DataThistleTime.parse(null))
        assertNull(DataThistleTime.parse("  "))
        assertNull(DataThistleTime.parse("next Friday"))
    }
}
