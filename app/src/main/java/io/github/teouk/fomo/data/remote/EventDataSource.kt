package io.github.teouk.fomo.data.remote

import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter

/**
 * Anywhere events can come from. DataThistleDataSource implements it for the app, and
 * the unit tests implement it with a fake. EventRepository only ever talks to this
 * interface, so swapping the events API would change one line in AppContainer and no
 * screen. That is how FOMO moved from Ticketmaster to Data Thistle.
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
