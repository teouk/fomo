package io.github.teouk.fomo.data.remote.datathistle

import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.Venue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId

private val UK = ZoneId.of("Europe/London")
private const val BOOKING_LINK_TYPE = "booking"

/**
 * Converts a Data Thistle event into the app's [Event].
 *
 * Data Thistle nests things deeper than Ticketmaster: an event has schedules (one per
 * venue), each schedule has performances (one per date/time), and each performance has
 * tickets and booking links. FOMO shows one card per event, so we pick the next upcoming
 * performance and describe the event by it.
 *
 * @param now used to choose the next performance that has not started yet.
 */
fun DtEventDto.toEvent(now: Instant): Event {
    val showings = schedules.flatMap { schedule ->
        schedule.performances.mapNotNull { performance ->
            DataThistleTime.parse(performance.ts)?.let { Showing(schedule, performance, it) }
        }
    }.sortedBy { it.startsAt }
    val next = showings.firstOrNull { !it.startsAt.isBefore(now) } ?: showings.lastOrNull()

    val schedule = next?.schedule ?: schedules.firstOrNull()
    val startsAt = next?.startsAt ?: DataThistleTime.parse(schedule?.startTs)
    val local = startsAt?.atZone(UK)

    // Cheapest ticket across every performance, so "From £x" is the lowest price on offer.
    val cheapest = schedules.flatMap { it.performances }.flatMap { it.tickets }
        .filter { it.minPrice != null }
        .minByOrNull { it.minPrice!! }

    val bookingLink = next?.performance?.links
        ?.firstOrNull { it.type == BOOKING_LINK_TYPE && !it.url.isNullOrBlank() }?.url
        ?: next?.performance?.links?.firstOrNull { !it.url.isNullOrBlank() }?.url

    return Event(
        id = eventId,
        name = name,
        localDate = local?.toLocalDate(),
        localTime = local?.toLocalTime(),
        startsAt = startsAt,
        category = tags.firstOrNull()?.replaceFirstChar { it.uppercase() },
        description = (descriptions ?: description)?.descriptionText(),
        imageUrl = images.maxByOrNull { it.width }?.url,
        ticketUrl = bookingLink ?: website,
        minPrice = cheapest?.minPrice,
        currency = cheapest?.currency,
        venue = schedule?.place?.toVenue(),
        distanceMiles = distance,
    )
}

private data class Showing(val schedule: DtScheduleDto, val performance: DtPerformanceDto, val startsAt: Instant)

private fun DtPlaceDto.toVenue(): Venue? {
    val venueName = name ?: return null
    return Venue(name = venueName, city = town, address = address, latitude = lat, longitude = lon)
}

/**
 * Descriptions arrive as a list of {type, description} objects in the spec, but we also
 * accept a plain string or a list of strings. The first non-blank text wins.
 */
internal fun JsonElement.descriptionText(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() }
    is JsonArray -> firstNotNullOfOrNull { it.descriptionText() }
    is JsonObject -> (this["description"] ?: this["text"])?.let { element ->
        (element as? JsonPrimitive)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }
}
