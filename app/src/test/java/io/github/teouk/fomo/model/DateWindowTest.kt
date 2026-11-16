package io.github.teouk.fomo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DateWindowTest {
    private val london = ZoneId.of("Europe/London")

    private fun londonTime(text: String): ZonedDateTime = ZonedDateTime.parse("$text[Europe/London]")

    @Test
    fun today_midAfternoon_runsFromNowToMidnight() {
        val now = londonTime("2026-11-11T15:30:00Z") // a Wednesday
        val window = DateWindow.today(now)
        assertEquals(Instant.parse("2026-11-11T15:30:00Z"), window.start)
        assertEquals(Instant.parse("2026-11-12T00:00:00Z"), window.end)
    }

    @Test
    fun weekend_onWednesday_coversComingSaturdayAndSunday() {
        val window = DateWindow.weekend(londonTime("2026-11-11T15:30:00Z"))
        assertEquals(Instant.parse("2026-11-14T00:00:00Z"), window.start)
        assertEquals(Instant.parse("2026-11-16T00:00:00Z"), window.end)
    }

    @Test
    fun weekend_onSaturdayEvening_startsNowAndEndsMondayMidnight() {
        val window = DateWindow.weekend(londonTime("2026-11-14T20:00:00Z"))
        assertEquals(Instant.parse("2026-11-14T20:00:00Z"), window.start)
        assertEquals(Instant.parse("2026-11-16T00:00:00Z"), window.end)
    }

    @Test
    fun weekend_onSunday_endsMondayMidnightOfSameWeekend() {
        val window = DateWindow.weekend(londonTime("2026-11-15T09:00:00Z"))
        assertEquals(Instant.parse("2026-11-16T00:00:00Z"), window.end)
    }

    @Test
    fun next7Days_includesTheSeventhDayInFull() {
        val window = DateWindow.next7Days(londonTime("2026-11-11T15:30:00Z"))
        assertEquals(Instant.parse("2026-11-19T00:00:00Z"), window.end)
    }

    @Test
    fun between_duringBritishSummerTime_usesLondonMidnightNotUtc() {
        // In July London is UTC+1, so local midnight is 23:00 UTC the day before.
        val window = DateWindow.between(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 11), london)
        assertEquals(Instant.parse("2026-07-09T23:00:00Z"), window.start)
        assertEquals(Instant.parse("2026-07-11T23:00:00Z"), window.end)
    }

    @Test
    fun constructor_endBeforeStart_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            DateWindow(Instant.parse("2026-11-12T00:00:00Z"), Instant.parse("2026-11-11T00:00:00Z"))
        }
    }
}
