package io.github.teouk.fomo.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ClashCheckerTest {
    // An event at 19:00; with the assumed 3 hours it runs until 22:00.
    private val eventStart = Instant.parse("2026-11-26T19:00:00Z")

    private fun entry(title: String, begin: String, end: String, allDay: Boolean = false, free: Boolean = false, declined: Boolean = false) =
        CalendarEntry(title, Instant.parse(begin), Instant.parse(end), allDay, free, declined)

    @Test
    fun findClash_lectureOverlappingStart_isClash() {
        val lecture = entry("CSC306 Lecture", "2026-11-26T18:00:00Z", "2026-11-26T20:00:00Z")
        assertEquals(Clash("CSC306 Lecture", lecture.begin, lecture.end), ClashChecker.findClash("Gig", eventStart, listOf(lecture)))
    }

    @Test
    fun findClash_entryInsideAssumedThreeHours_isClash() {
        val late = entry("Shift", "2026-11-26T21:30:00Z", "2026-11-26T23:00:00Z")
        assertEquals("Shift", ClashChecker.findClash("Gig", eventStart, listOf(late))?.title)
    }

    @Test
    fun findClash_entryEndingExactlyAtStart_isNotClash() {
        val before = entry("Seminar", "2026-11-26T17:00:00Z", "2026-11-26T19:00:00Z")
        assertNull(ClashChecker.findClash("Gig", eventStart, listOf(before)))
    }

    @Test
    fun findClash_entryAfterThreeHours_isNotClash() {
        val after = entry("Night bus", "2026-11-26T22:00:00Z", "2026-11-26T23:00:00Z")
        assertNull(ClashChecker.findClash("Gig", eventStart, listOf(after)))
    }

    @Test
    fun findClash_allDayFreeOrDeclined_areIgnored() {
        val entries = listOf(
            entry("Birthday", "2026-11-26T00:00:00Z", "2026-11-27T00:00:00Z", allDay = true),
            entry("Maybe drinks", "2026-11-26T19:00:00Z", "2026-11-26T21:00:00Z", free = true),
            entry("Declined meeting", "2026-11-26T19:00:00Z", "2026-11-26T20:00:00Z", declined = true),
        )
        assertNull(ClashChecker.findClash("Gig", eventStart, entries))
    }

    @Test
    fun findClash_eventAlreadyInCalendar_isNotClashWithItself() {
        val same = entry("Jazz on the Bay", "2026-11-26T19:00:00Z", "2026-11-26T22:00:00Z")
        assertNull(ClashChecker.findClash("Jazz on the Bay", eventStart, listOf(same)))
    }

    @Test
    fun findClash_twoOverlaps_returnsEarliest() {
        val entries = listOf(
            entry("Later", "2026-11-26T20:00:00Z", "2026-11-26T21:00:00Z"),
            entry("Earlier", "2026-11-26T18:30:00Z", "2026-11-26T19:30:00Z"),
        )
        assertEquals("Earlier", ClashChecker.findClash("Gig", eventStart, entries)?.title)
    }
}
