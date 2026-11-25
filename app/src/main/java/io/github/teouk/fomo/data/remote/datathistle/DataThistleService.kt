package io.github.teouk.fomo.data.remote.datathistle

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * EXTERNAL API: Data Thistle Publishing API v1 (https://api.datathistle.com/).
 * UK live-events listings: events, venues ("places"), performances and tickets.
 * Used for: Discover, Search & Filter, Event Details and the Map tab.
 *
 * Authentication: an access token sent as "Authorization: Bearer <token>" on every
 * request (added by an OkHttp interceptor in ApiClients, never in this interface).
 * Terms: results may be cached for at most 24 hours, and the data must be credited
 * to Data Thistle on screen. Pages return at most 20 events.
 */
interface DataThistleService {
    /** GET /events: events near a point, within a date range, optionally by tag or name. */
    @GET("events")
    suspend fun events(
        @QueryMap query: Map<String, String>,
        @Query("tags") tag: String? = null,
    ): List<DtEventDto>

    /** GET /event/{id}: one event with its schedules, performances and tickets. */
    @GET("event/{eventId}")
    suspend fun event(@Path("eventId") eventId: String): DtEventDto

    companion object {
        const val BASE_URL = "https://api.datathistle.com/v1/"

        /** The API's maximum page size. */
        const val MAX_PAGE_SIZE = 20
    }
}
