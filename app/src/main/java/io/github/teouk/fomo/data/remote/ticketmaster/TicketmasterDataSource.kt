package io.github.teouk.fomo.data.remote.ticketmaster

import io.github.teouk.fomo.data.remote.EventDataSource
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter

/** Events from the Ticketmaster Discovery API v2 (or the offline sample, which speaks the same format). */
class TicketmasterDataSource(
    private val service: TicketmasterService,
    override val providerName: String = "Ticketmaster",
) : EventDataSource {
    override suspend fun search(filter: EventFilter): List<Event> =
        service.searchEvents(EventQueryBuilder.build(filter)).embedded?.events.orEmpty().map { it.toEvent() }

    override suspend fun getEvent(eventId: String): Event = service.getEvent(eventId).toEvent()
}
