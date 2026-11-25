package io.github.teouk.fomo

import io.github.teouk.fomo.data.remote.EventDataSource
import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter
import io.github.teouk.fomo.model.Venue
import java.time.Instant
import java.time.ZoneId

/**
 * A test-only stand-in for the Data Thistle API. It never touches the network and
 * applies the filter itself, so ViewModel tests are fast and predictable.
 * It lives in src/test, so it is never part of the app.
 */
class FakeEventDataSource(var events: List<Pair<Event, Category>> = emptyList()) : EventDataSource {
    override val providerName = "Fake"
    var searchCalls = 0
    val searchedFilters = mutableListOf<EventFilter>()

    override suspend fun search(filter: EventFilter): List<Event> {
        searchCalls++
        searchedFilters += filter
        return events
            .filter { (_, category) -> filter.categories.isEmpty() || category in filter.categories }
            .filter { (event, _) -> filter.keyword.isBlank() || event.name.contains(filter.keyword.trim(), ignoreCase = true) }
            .filter { (event, _) ->
                val window = filter.dateWindow
                val start = event.startsAt
                window == null || (start != null && start >= window.start && start < window.end)
            }
            .map { it.first }
    }

    override suspend fun getEvent(eventId: String): Event = events.first { it.first.id == eventId }.first

    companion object {
        private val UK = ZoneId.of("Europe/London")

        fun event(id: String, name: String, startsAt: String, venue: Venue? = null): Event {
            val instant = Instant.parse(startsAt)
            val local = instant.atZone(UK)
            return Event(
                id = id, name = name, localDate = local.toLocalDate(), localTime = local.toLocalTime(),
                startsAt = instant, category = null, description = null, imageUrl = null, ticketUrl = null,
                minPrice = null, currency = null, venue = venue,
            )
        }
    }
}
