package io.github.teouk.fomo.data.remote.datathistle

import io.github.teouk.fomo.data.remote.EventDataSource
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Clock

/** Events from the Data Thistle API. */
class DataThistleDataSource(
    private val service: DataThistleService,
    private val clock: Clock = Clock.systemUTC(),
) : EventDataSource {
    override val providerName = "Data Thistle"

    override suspend fun search(filter: EventFilter): List<Event> {
        val now = clock.instant()
        val query = DataThistleQueryBuilder.build(filter, now)
        val tags = filter.categories.map(DataThistleQueryBuilder::tagFor)

        // Data Thistle's `tags` means "must have ALL of these", but our chips mean "ANY of
        // these". So we send one request per category, in parallel, and merge the results.
        val pages = if (tags.isEmpty()) {
            listOf(service.events(query))
        } else {
            coroutineScope { tags.map { tag -> async { service.events(query, tag) } }.awaitAll() }
        }

        return pages.flatten()
            .distinctBy { it.eventId }
            .map { it.toEvent(now) }
            .sortedWith(compareBy(nullsLast()) { it.startsAt })
    }

    override suspend fun getEvent(eventId: String): Event = service.event(eventId).toEvent(clock.instant())
}
