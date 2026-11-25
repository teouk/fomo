package io.github.teouk.fomo.data.remote

import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter

/**
 * Anywhere events can come from. Each events API (Data Thistle, Ticketmaster) and the
 * offline sample data implement this interface, and EventRepository only ever talks to
 * the interface. Swapping the API therefore changes one line in AppContainer and no screen.
 *
 * Implementations may throw IOException (network), retrofit2.HttpException (an HTTP
 * error code) or SerializationException (unreadable JSON); EventRepository turns these
 * into a DataError.
 */
interface EventDataSource {
    /** The provider's name, shown on screen as the data credit. */
    val providerName: String

    suspend fun search(filter: EventFilter): List<Event>

    suspend fun getEvent(eventId: String): Event
}
