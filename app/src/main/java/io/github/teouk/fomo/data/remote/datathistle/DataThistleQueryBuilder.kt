package io.github.teouk.fomo.data.remote.datathistle

import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.EventFilter
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Turns an [EventFilter] into Data Thistle `/events` query parameters. Pure Kotlin, unit-tested. */
object DataThistleQueryBuilder {
    /** Without a date filter, "upcoming" means the next four weeks. */
    val DEFAULT_LOOKAHEAD: Duration = Duration.ofDays(28)

    /**
     * Our chips map to Data Thistle tags. TODO(api-token): check each tag against
     * real responses; the spec's examples include "music", "film", "comedy", "sport", "kids".
     */
    fun tagFor(category: Category): String = when (category) {
        Category.MUSIC -> "music"
        Category.SPORT -> "sport"
        Category.ARTS_THEATRE -> "theatre"
        Category.COMEDY -> "comedy"
        Category.FAMILY -> "kids"
        Category.FILM -> "film"
    }

    fun build(filter: EventFilter, now: Instant, page: Int = 1): Map<String, String> {
        require(page >= 1) { "Data Thistle pages start at 1" }
        val window = filter.dateWindow
        val start = window?.start?.let { maxOf(it, now) } ?: now
        val end = window?.end ?: now.plus(DEFAULT_LOOKAHEAD)

        return buildMap {
            put("lat", filter.latitude.toString())
            put("lon", filter.longitude.toString())
            put("distance", filter.radiusMiles.toString())
            put("min_date", start.toApiDateTime())
            put("max_date", end.toApiDateTime())
            put("limit", DataThistleService.MAX_PAGE_SIZE.toString())
            put("page", page.toString())
            filter.keyword.trim().takeIf { it.isNotEmpty() }?.let { put("name", it) }
        }
    }

    private fun Instant.toApiDateTime(): String = DateTimeFormatter.ISO_INSTANT.format(truncatedTo(ChronoUnit.SECONDS))
}
