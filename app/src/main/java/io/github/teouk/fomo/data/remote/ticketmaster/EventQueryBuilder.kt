package io.github.teouk.fomo.data.remote.ticketmaster

import io.github.teouk.fomo.model.EventFilter
import io.github.teouk.fomo.util.GeoHash
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Turns an [EventFilter] into the query parameters of Ticketmaster's event search
 * (`GET /discovery/v2/events.json`). Pure Kotlin, so the exact parameters are unit-tested.
 */
object EventQueryBuilder {
    const val DEFAULT_PAGE_SIZE = 50

    /** Ticketmaster rejects deep paging: size * page must stay below this. */
    const val MAX_PAGED_RESULTS = 1000

    private const val UNIT_MILES = "miles"
    private const val SORT_SOONEST_FIRST = "date,asc"

    fun build(filter: EventFilter, page: Int = 0, pageSize: Int = DEFAULT_PAGE_SIZE): Map<String, String> {
        require(page >= 0 && pageSize > 0) { "page and pageSize must be positive" }
        require(page * pageSize < MAX_PAGED_RESULTS) { "Ticketmaster only pages through the first $MAX_PAGED_RESULTS results" }

        // buildMap creates a read-only Map; inside the braces we can still add entries.
        return buildMap {
            put("geoPoint", GeoHash.encode(filter.latitude, filter.longitude))
            put("radius", filter.radiusMiles.toString())
            put("unit", UNIT_MILES)
            put("sort", SORT_SOONEST_FIRST)
            put("size", pageSize.toString())
            put("page", page.toString())

            val keyword = filter.keyword.trim()
            if (keyword.isNotEmpty()) put("keyword", keyword)

            if (filter.categories.isNotEmpty()) {
                // A comma-separated list means "any of these". Sorted so the same filter
                // always produces the same request, which makes caching reliable.
                put("classificationName", filter.categories.map { it.classificationName }.sorted().joinToString(","))
            }

            filter.dateWindow?.let { window ->
                put("startDateTime", window.start.toApiDateTime())
                put("endDateTime", window.end.toApiDateTime())
            }
        }
    }

    /** Ticketmaster wants ISO-8601 in UTC without milliseconds, e.g. 2026-11-13T18:00:00Z. */
    private fun Instant.toApiDateTime(): String =
        DateTimeFormatter.ISO_INSTANT.format(truncatedTo(ChronoUnit.SECONDS))
}
