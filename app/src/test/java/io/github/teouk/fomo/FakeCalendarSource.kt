package io.github.teouk.fomo

import io.github.teouk.fomo.data.calendar.CalendarEntry
import io.github.teouk.fomo.data.calendar.CalendarSource
import java.time.Instant

/** A pretend phone calendar for tests: no Android, no permission dialogs. */
class FakeCalendarSource(
    var entries: List<CalendarEntry> = emptyList(),
    var permissionGranted: Boolean = true,
) : CalendarSource {
    override fun hasPermission() = permissionGranted

    override suspend fun entriesBetween(from: Instant, to: Instant) = entries.filter { it.begin < to && it.end > from }
}
