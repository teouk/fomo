package io.github.teouk.fomo.data.remote.datathistle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * DTOs mirroring Data Thistle's JSON (schemas EventSuccess and PlaceSuccess in the
 * OpenAPI spec at https://api.datathistle.com/openapi/openapi.yaml). The "Dt" prefix
 * keeps them apart from Ticketmaster's DTOs.
 *
 * Every field has a default because the API leaves out what it does not know. Fields
 * the spec marks "@todo" or describes inconsistently are read as raw JsonElement and
 * interpreted leniently by the mapper, so an unexpected shape cannot crash the parse.
 */

@Serializable
data class DtEventDto(
    @SerialName("event_id") val eventId: String,
    val name: String = "",
    val website: String? = null,
    val tags: List<String> = emptyList(),
    /** The spec names this both "description" and "descriptions"; we accept either. */
    val description: JsonElement? = null,
    val descriptions: JsonElement? = null,
    val images: List<DtImageDto> = emptyList(),
    val schedules: List<DtScheduleDto> = emptyList(),
)

@Serializable
data class DtImageDto(
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("alt_text") val altText: String? = null,
)

@Serializable
data class DtScheduleDto(
    @SerialName("place_id") val placeId: String? = null,
    @SerialName("start_ts") val startTs: String? = null,
    @SerialName("end_ts") val endTs: String? = null,
    val place: DtPlaceDto? = null,
    val performances: List<DtPerformanceDto> = emptyList(),
    /** Miles from the searched point, present when searching with lat/lon/distance. */
    @SerialName("_distance") val distance: Double? = null,
)

@Serializable
data class DtPlaceDto(
    @SerialName("place_id") val placeId: String? = null,
    val name: String? = null,
    val address: String? = null,
    val town: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("_distance") val distance: Double? = null,
)

@Serializable
data class DtPerformanceDto(
    val ts: String? = null,
    val links: List<DtLinkDto> = emptyList(),
    val tickets: List<DtTicketDto> = emptyList(),
)

@Serializable
data class DtLinkDto(
    val url: String? = null,
    val type: String? = null,
)

@Serializable
data class DtTicketDto(
    val type: String? = null,
    val currency: String? = null,
    @SerialName("min_price") val minPrice: Double? = null,
    @SerialName("max_price") val maxPrice: Double? = null,
)
