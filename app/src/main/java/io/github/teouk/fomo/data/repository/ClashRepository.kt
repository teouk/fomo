package io.github.teouk.fomo.data.repository

import io.github.teouk.fomo.data.calendar.CalendarSource
import io.github.teouk.fomo.data.calendar.Clash
import io.github.teouk.fomo.data.calendar.ClashChecker
import io.github.teouk.fomo.model.Event
import kotlinx.coroutines.flow.first

/**
 * Calendar Clash Check (bonus feature): which events overlap the user's calendar.
 * Returns nothing when the feature is switched off in Profile or the calendar
 * permission is missing, so screens never need to check those themselves.
 */
class ClashRepository(
    private val calendarRepository: CalendarSource,
    private val settings: SettingsStore,
) {
    /** @return event ID → clash, only for the events that clash. */
    suspend fun clashesFor(events: List<Event>): Map<String, Clash> {
        if (!settings.clashCheckEnabled.first() || !calendarRepository.hasPermission()) return emptyMap()
        val starts = events.mapNotNull { it.startsAt }
        if (starts.isEmpty()) return emptyMap()

        // One calendar query covering every event, then each event is checked in memory.
        val entries = calendarRepository.entriesBetween(
            from = starts.min(),
            to = starts.max().plus(ClashChecker.ASSUMED_EVENT_DURATION),
        )
        return events.mapNotNull { event ->
            val start = event.startsAt ?: return@mapNotNull null
            ClashChecker.findClash(event.name, start, entries)?.let { event.id to it }
        }.toMap()
    }
}
