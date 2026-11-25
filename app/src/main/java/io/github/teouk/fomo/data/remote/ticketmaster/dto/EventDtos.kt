package io.github.teouk.fomo.data.remote.ticketmaster.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * DTOs (data transfer objects) mirror the JSON returned by the Ticketmaster Discovery
 * API v2 (https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/).
 * They exist only to be parsed; EventMapper converts them into the app's own Event model.
 *
 * Every field has a default, because Ticketmaster omits fields it has no data for.
 */

@Serializable
data class EventSearchResponseDto(
    @SerialName("_embedded") val embedded: EmbeddedEventsDto? = null,
    val page: PageDto? = null,
)

@Serializable
data class EmbeddedEventsDto(
    val events: List<EventDto> = emptyList(),
)

@Serializable
data class PageDto(
    val size: Int = 0,
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
)

@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val url: String? = null,
    val info: String? = null,
    val description: String? = null,
    val images: List<ImageDto> = emptyList(),
    val dates: DatesDto? = null,
    val classifications: List<ClassificationDto> = emptyList(),
    val priceRanges: List<PriceRangeDto> = emptyList(),
    @SerialName("_embedded") val embedded: EmbeddedVenuesDto? = null,
)

@Serializable
data class ImageDto(
    val url: String,
    val ratio: String? = null,
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class DatesDto(
    val start: StartDto? = null,
)

@Serializable
data class StartDto(
    val localDate: String? = null,
    val localTime: String? = null,
    val dateTime: String? = null,
)

@Serializable
data class ClassificationDto(
    val primary: Boolean = false,
    val segment: NamedDto? = null,
    val genre: NamedDto? = null,
)

@Serializable
data class NamedDto(
    val name: String? = null,
)

@Serializable
data class PriceRangeDto(
    val type: String? = null,
    val currency: String? = null,
    val min: Double? = null,
    val max: Double? = null,
)

@Serializable
data class EmbeddedVenuesDto(
    val venues: List<VenueDto> = emptyList(),
)

@Serializable
data class VenueDto(
    val name: String? = null,
    val city: NamedDto? = null,
    val address: AddressDto? = null,
    val postalCode: String? = null,
    val location: LocationDto? = null,
)

@Serializable
data class AddressDto(
    val line1: String? = null,
)

@Serializable
data class LocationDto(
    // Ticketmaster sends coordinates as text, e.g. "51.6144".
    val latitude: String? = null,
    val longitude: String? = null,
)
