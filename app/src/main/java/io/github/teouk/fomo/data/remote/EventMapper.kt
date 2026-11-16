package io.github.teouk.fomo.data.remote

import io.github.teouk.fomo.data.remote.dto.EventDto
import io.github.teouk.fomo.data.remote.dto.ImageDto
import io.github.teouk.fomo.data.remote.dto.VenueDto
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.Venue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** Image ratio used by both card layouts and the details header. */
private const val PREFERRED_IMAGE_RATIO = "16_9"

/** Wide enough for a full-width phone photo without downloading the largest file. */
private const val PREFERRED_IMAGE_WIDTH_PX = 1024

/** Ticketmaster uses this name when it has no category for an event. */
private const val UNDEFINED_CLASSIFICATION = "Undefined"

/**
 * Converts a Ticketmaster DTO into the app's [Event].
 *
 * Written as an extension function: it is called as `dto.toEvent()` but lives outside
 * the DTO class, so the DTO stays a plain description of the JSON.
 */
fun EventDto.toEvent(): Event {
    val start = dates?.start
    val cheapestPrice = priceRanges.filter { it.min != null }.minByOrNull { it.min!! }
    val classification = classifications.firstOrNull { it.primary } ?: classifications.firstOrNull()

    return Event(
        id = id,
        name = name,
        localDate = start?.localDate.parseOrNull(LocalDate::parse),
        localTime = start?.localTime.parseOrNull(LocalTime::parse),
        startsAt = start?.dateTime.parseOrNull(Instant::parse),
        category = (classification?.segment?.name ?: classification?.genre?.name)
            ?.takeUnless { it == UNDEFINED_CLASSIFICATION },
        description = description ?: info,
        imageUrl = images.bestImage()?.url,
        ticketUrl = url,
        minPrice = cheapestPrice?.min,
        currency = cheapestPrice?.currency,
        venue = embedded?.venues?.firstOrNull()?.toVenue(),
    )
}

private fun VenueDto.toVenue(): Venue? {
    val venueName = name ?: return null
    return Venue(
        name = venueName,
        city = city?.name,
        address = address?.line1,
        latitude = location?.latitude?.toDoubleOrNull(),
        longitude = location?.longitude?.toDoubleOrNull(),
    )
}

/**
 * Picks the smallest 16:9 image that is at least [PREFERRED_IMAGE_WIDTH_PX] wide.
 * Falls back to the widest 16:9 image, then to any image at all.
 */
private fun List<ImageDto>.bestImage(): ImageDto? {
    val wide = filter { it.ratio == PREFERRED_IMAGE_RATIO }
    return wide.filter { it.width >= PREFERRED_IMAGE_WIDTH_PX }.minByOrNull { it.width }
        ?: wide.maxByOrNull { it.width }
        ?: maxByOrNull { it.width }
}

/** Parses the text with [parser], or returns null if it is missing or malformed. */
private fun <T> String?.parseOrNull(parser: (String) -> T): T? {
    if (this == null) return null
    return try {
        parser(this)
    } catch (e: DateTimeParseException) {
        null
    }
}
