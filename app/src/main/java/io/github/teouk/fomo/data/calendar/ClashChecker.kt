package io.github.teouk.fomo.data.calendar

import java.time.Duration
import java.time.Instant

/** One occurrence of an entry in the phone's calendar (a single week of a weekly lecture, say). */
data class CalendarEntry(
    val title: String,
    val begin: Instant,
    val end: Instant,
    val isAllDay: Boolean = false,
    /** The owner marked this time as "free" (Show as: Free), so it does not block anything. */
    val isFree: Boolean = false,
    /** The user declined this invitation. */
    val isDeclined: Boolean = false,
)

/** A calendar entry that overlaps an event. */
data class Clash(val title: String, val begin: Instant, val end: Instant)

/**
 * Decides whether an event may clash with the user's calendar. Pure Kotlin, so every rule
 * is unit-tested without a phone.
 *
 * Listings rarely give an end time, so an event is assumed to last
 * [ASSUMED_EVENT_DURATION]; that is why the app says "May clash" rather than "Busy".
 */
object ClashChecker {
    val ASSUMED_EVENT_DURATION: Duration = Duration.ofHours(3)

    /** @return the first entry that overlaps the event, or null if the user is free. */
    fun findClash(eventName: String, eventStart: Instant, entries: List<CalendarEntry>): Clash? {
        val eventEnd = eventStart.plus(ASSUMED_EVENT_DURATION)
        return entries
            .asSequence()
            // All-day entries (birthdays, holidays) and "free" time never block an evening out.
            .filterNot { it.isAllDay || it.isFree || it.isDeclined }
            // The event itself, if the user already added it with "Add to calendar".
            .filterNot { it.title.equals(eventName, ignoreCase = true) }
            // Two time ranges overlap when each starts before the other ends.
            .filter { it.begin < eventEnd && it.end > eventStart }
            .sortedBy { it.begin }
            .firstOrNull()
            ?.let { Clash(it.title, it.begin, it.end) }
    }
}
