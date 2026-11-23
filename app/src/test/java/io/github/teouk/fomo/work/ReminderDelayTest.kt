package io.github.teouk.fomo.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ReminderDelayTest {
    private val now = Instant.parse("2026-11-13T12:00:00Z")

    @Test
    fun reminderDelay_eventTomorrow_remindsLeadHoursBefore() {
        val start = Instant.parse("2026-11-14T19:00:00Z")
        assertEquals(Duration.ofHours(29), ReminderScheduler.reminderDelay(start, leadHours = 2, now = now))
    }

    @Test
    fun reminderDelay_insideLeadTime_remindsImmediately() {
        val start = Instant.parse("2026-11-13T13:00:00Z")
        assertEquals(Duration.ZERO, ReminderScheduler.reminderDelay(start, leadHours = 2, now = now))
    }

    @Test
    fun reminderDelay_eventStarted_returnsNull() {
        assertNull(ReminderScheduler.reminderDelay(now, leadHours = 2, now = now))
    }
}
