package io.github.teouk.fomo.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** A period of time to search within. [start] is inclusive, [end] is exclusive. */
data class DateWindow(val start: Instant, val end: Instant) {
    init {
        require(start < end) { "start must be before end" }
    }

    companion object {
        const val UPCOMING_DAYS = 7L

        /** From [now] until midnight tonight. */
        fun today(now: ZonedDateTime): DateWindow =
            DateWindow(now.toInstant(), startOfDay(now.toLocalDate().plusDays(1), now.zone))

        /**
         * The coming Saturday and Sunday. If it is already the weekend, the window starts
         * [now] so events that have finished are not suggested.
         */
        fun weekend(now: ZonedDateTime): DateWindow {
            val today = now.toLocalDate()
            val isWeekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
            val monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            val start = if (isWeekend) {
                now.toInstant()
            } else {
                startOfDay(today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)), now.zone)
            }
            return DateWindow(start, startOfDay(monday, now.zone))
        }

        /** From [now] until midnight at the end of the 7th day from today. */
        fun next7Days(now: ZonedDateTime): DateWindow =
            DateWindow(now.toInstant(), startOfDay(now.toLocalDate().plusDays(UPCOMING_DAYS + 1), now.zone))

        /** A range chosen in the date picker. Both days are included in full. */
        fun between(firstDay: LocalDate, lastDay: LocalDate, zone: ZoneId): DateWindow =
            DateWindow(startOfDay(firstDay, zone), startOfDay(lastDay.plusDays(1), zone))

        private fun startOfDay(date: LocalDate, zone: ZoneId): Instant =
            ZonedDateTime.of(date, LocalTime.MIDNIGHT, zone).toInstant()
    }
}
