package io.github.teouk.fomo.data.repository

import io.github.teouk.fomo.data.remote.EventQueryBuilder
import io.github.teouk.fomo.data.remote.TicketmasterService
import io.github.teouk.fomo.data.remote.toEvent
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.EventFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * The single place the rest of the app asks for events. ViewModels call this and never
 * touch Retrofit, so the data source can change without changing any screen.
 *
 * Recent results are kept in memory for [CACHE_TTL] to stay inside Ticketmaster's quota
 * and to make going back to a screen instant.
 *
 * Its dependencies arrive through the constructor ("dependency injection"), which lets
 * the unit tests pass in a fake service and a fake clock.
 */
class EventRepository(
    private val service: TicketmasterService,
    private val hasApiKey: Boolean,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private class CacheEntry<T>(val value: T, val savedAt: Instant)

    private val searchCache = mutableMapOf<Map<String, String>, CacheEntry<List<Event>>>()
    private val eventCache = mutableMapOf<String, CacheEntry<Event>>()

    /** Guards the two maps, because several coroutines may load at the same time. */
    private val cacheMutex = Mutex()

    suspend fun searchEvents(filter: EventFilter, forceRefresh: Boolean = false): Outcome<List<Event>> {
        val query = EventQueryBuilder.build(filter)
        if (!forceRefresh) {
            cacheMutex.withLock { searchCache[query]?.takeIf { it.isFresh() } }
                ?.let { return Outcome.Success(it.value) }
        }
        return load {
            val events = service.searchEvents(query).embedded?.events.orEmpty().map { it.toEvent() }
            cacheMutex.withLock {
                val now = clock.instant()
                searchCache[query] = CacheEntry(events, now)
                // Search results already hold each event, so opening Details needs no request.
                events.forEach { eventCache[it.id] = CacheEntry(it, now) }
            }
            events
        }
    }

    suspend fun getEvent(eventId: String): Outcome<Event> {
        cacheMutex.withLock { eventCache[eventId]?.takeIf { it.isFresh() } }
            ?.let { return Outcome.Success(it.value) }
        return load {
            val event = service.getEvent(eventId).toEvent()
            cacheMutex.withLock { eventCache[eventId] = CacheEntry(event, clock.instant()) }
            event
        }
    }

    private fun CacheEntry<*>.isFresh(): Boolean =
        Duration.between(savedAt, clock.instant()) < CACHE_TTL

    /** Runs [request] off the main thread and converts every failure into a [DataError]. */
    private suspend fun <T> load(request: suspend () -> T): Outcome<T> {
        if (!hasApiKey) return Outcome.Failure(DataError.INVALID_API_KEY)
        return withContext(ioDispatcher) {
            try {
                Outcome.Success(request())
            } catch (e: CancellationException) {
                // Cancellation is how coroutines stop work nobody needs any more
                // (for example the user left the screen). It must not be swallowed.
                throw e
            } catch (e: HttpException) {
                Outcome.Failure(e.toDataError())
            } catch (e: IOException) {
                Outcome.Failure(DataError.OFFLINE)
            } catch (e: SerializationException) {
                Outcome.Failure(DataError.SERVER)
            }
        }
    }

    private fun HttpException.toDataError(): DataError = when (code()) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> DataError.INVALID_API_KEY
        HTTP_NOT_FOUND -> DataError.NOT_FOUND
        HTTP_TOO_MANY_REQUESTS -> DataError.RATE_LIMITED
        else -> DataError.SERVER
    }

    companion object {
        /** Listings change slowly, so ten minutes is fresh enough. */
        val CACHE_TTL: Duration = Duration.ofMinutes(10)

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
