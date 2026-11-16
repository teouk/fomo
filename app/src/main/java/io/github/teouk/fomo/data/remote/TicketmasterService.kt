package io.github.teouk.fomo.data.remote

import io.github.teouk.fomo.data.remote.dto.EventDto
import io.github.teouk.fomo.data.remote.dto.EventSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * EXTERNAL API: Ticketmaster Discovery API v2.
 * Docs: https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/
 * Used for: searching events near a location and loading one event's details.
 * Limits on the free key: 5,000 calls a day and 5 a second, so results are cached.
 *
 * Retrofit generates the implementation of this interface. `suspend` means each call
 * pauses the coroutine while the network request runs, without blocking a thread.
 */
interface TicketmasterService {
    @GET("discovery/v2/events.json")
    suspend fun searchEvents(@QueryMap query: Map<String, String>): EventSearchResponseDto

    @GET("discovery/v2/events/{id}.json")
    suspend fun getEvent(@Path("id") eventId: String): EventDto
}
