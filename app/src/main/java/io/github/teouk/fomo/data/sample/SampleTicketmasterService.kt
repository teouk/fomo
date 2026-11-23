package io.github.teouk.fomo.data.sample

import io.github.teouk.fomo.data.remote.TicketmasterService
import io.github.teouk.fomo.data.remote.dto.ClassificationDto
import io.github.teouk.fomo.data.remote.dto.DatesDto
import io.github.teouk.fomo.data.remote.dto.EmbeddedEventsDto
import io.github.teouk.fomo.data.remote.dto.EmbeddedVenuesDto
import io.github.teouk.fomo.data.remote.dto.EventDto
import io.github.teouk.fomo.data.remote.dto.EventSearchResponseDto
import io.github.teouk.fomo.data.remote.dto.ImageDto
import io.github.teouk.fomo.data.remote.dto.NamedDto
import io.github.teouk.fomo.data.remote.dto.PageDto
import io.github.teouk.fomo.data.remote.dto.PriceRangeDto
import io.github.teouk.fomo.data.remote.dto.StartDto
import io.github.teouk.fomo.data.remote.dto.VenueDto
import io.github.teouk.fomo.data.remote.dto.AddressDto
import io.github.teouk.fomo.data.remote.dto.LocationDto
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * An offline stand-in for Ticketmaster, used only when no API key is configured.
 *
 * It implements the same [TicketmasterService] interface and returns the same DTOs, so
 * the repository, mapper and every screen run exactly as they would with live data.
 * This is the payoff of depending on an interface: the data source can be swapped
 * without changing any caller.
 *
 * Dates are generated relative to today, so the sample events are always upcoming.
 * The events are the eight Swansea events from the CW1 design's sample data.
 */
class SampleTicketmasterService(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.of("Europe/London"),
) : TicketmasterService {

    private enum class Anchor { TODAY, NEXT_WEEKEND }

    private data class SampleEvent(
        val id: String,
        val name: String,
        val segment: String,
        val anchor: Anchor,
        val dayOffset: Long,
        val time: LocalTime,
        val venue: SampleVenue,
        val minPrice: Double?,
        val image: String,
        val description: String,
    )

    private data class SampleVenue(val name: String, val address: String, val latitude: Double, val longitude: Double)

    private val arena = SampleVenue("Swansea Arena", "Oystermouth Road", 51.6144, -3.9436)
    private val grand = SampleVenue("Swansea Grand Theatre", "Singleton Street", 51.6175, -3.9466)
    private val brangwyn = SampleVenue("Brangwyn Hall", "Guildhall, St Helen's Road", 51.6127, -3.9617)
    private val taliesin = SampleVenue("Taliesin Arts Centre", "Singleton Park", 51.6097, -3.9819)
    private val castle = SampleVenue("Oystermouth Castle", "Castle Avenue, Mumbles", 51.5770, -4.0019)
    private val sportsPark = SampleVenue("Swansea Bay Sports Park", "Sketty Lane", 51.6062, -3.9890)

    private val events = listOf(
        SampleEvent(
            "sample-01", "Jazz on the Bay", "Music", Anchor.TODAY, 0, LocalTime.of(19, 30), arena, 35.0,
            "event_01_jazz.jpg",
            "An evening of live jazz on the waterfront, with the Bay Big Band playing swing classics and new " +
                "arrangements. Doors open at 18:30 and the bar serves food until 19:15. The show runs for about " +
                "two and a half hours, including an interval. Suitable for ages 12 and over.",
        ),
        SampleEvent(
            "sample-02", "Fireworks at the Castle", "Family", Anchor.NEXT_WEEKEND, 0, LocalTime.of(18, 0), castle, 8.0,
            "event_02_fireworks.jpg",
            "A family fireworks display in the grounds of Oystermouth Castle, with food stalls and a lantern parade " +
                "before the show.",
        ),
        SampleEvent(
            "sample-03", "Comedy Club Night", "Comedy", Anchor.NEXT_WEEKEND, 0, LocalTime.of(20, 0), grand, 12.0,
            "event_03_comedy.jpg",
            "Four of the best touring stand-ups and a local compere. Contains strong language; ages 16 and over.",
        ),
        SampleEvent(
            "sample-04", "Five-a-side Charity Cup", "Sports", Anchor.NEXT_WEEKEND, 1, LocalTime.of(11, 0), sportsPark, 5.0,
            "event_04_football.jpg",
            "Sixteen local teams play a knockout five-a-side tournament in aid of a children's hospice. Spectators welcome.",
        ),
        SampleEvent(
            "sample-05", "Open Mic Night", "Music", Anchor.TODAY, 2, LocalTime.of(19, 0), taliesin, 0.0,
            "event_05_open_mic.jpg",
            "Bring an instrument or just come to listen. Sign-up opens at 18:30 for ten-minute slots.",
        ),
        SampleEvent(
            "sample-06", "Indie Night: The Harbour Lights", "Music", Anchor.TODAY, 4, LocalTime.of(20, 0), brangwyn, 15.0,
            "event_06_indie_gig.jpg",
            "Swansea's own The Harbour Lights play songs from their new album, with support from two local bands.",
        ),
        SampleEvent(
            "sample-07", "Film Night: Classic Noir", "Film", Anchor.TODAY, 6, LocalTime.of(19, 0), taliesin, 7.0,
            "event_07_cinema.jpg",
            "A double bill of 1940s film noir on the big screen, introduced by a lecturer from the university's film school.",
        ),
        SampleEvent(
            "sample-08", "The Nutcracker", "Arts & Theatre", Anchor.TODAY, 10, LocalTime.of(14, 30), grand, 22.0,
            "event_08_ballet.jpg",
            "A touring ballet company performs Tchaikovsky's festive classic with a live orchestra.",
        ),
    )

    override suspend fun searchEvents(query: Map<String, String>): EventSearchResponseDto {
        val keyword = query["keyword"]?.lowercase()
        val segments = query["classificationName"]?.split(",")?.toSet()
        val start = query["startDateTime"]?.let(Instant::parse)
        val end = query["endDateTime"]?.let(Instant::parse)
        val now = clock.instant()

        val matches = events
            .map { it.toDto() }
            .filter { dto ->
                val startsAt = Instant.parse(dto.dates!!.start!!.dateTime!!)
                startsAt >= now &&
                    (keyword == null || dto.name.lowercase().contains(keyword)) &&
                    (segments == null || dto.classifications.first().segment?.name in segments) &&
                    (start == null || startsAt >= start) &&
                    (end == null || startsAt < end)
            }
            .sortedBy { it.dates!!.start!!.dateTime }

        return EventSearchResponseDto(
            embedded = EmbeddedEventsDto(matches),
            page = PageDto(size = matches.size, totalElements = matches.size, totalPages = 1, number = 0),
        )
    }

    override suspend fun getEvent(eventId: String): EventDto =
        events.firstOrNull { it.id == eventId }?.toDto()
            ?: throw HttpException(Response.error<Any>(HTTP_NOT_FOUND, "".toResponseBody()))

    private fun SampleEvent.toDto(): EventDto {
        val today = LocalDate.now(clock.withZone(zone))
        val baseDay = when (anchor) {
            Anchor.TODAY -> today
            Anchor.NEXT_WEEKEND -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        }
        val day = baseDay.plusDays(dayOffset)
        val startsAt = ZonedDateTime.of(day, time, zone).toInstant()
        return EventDto(
            id = id,
            name = name,
            url = "https://www.ticketmaster.co.uk/",
            description = description,
            images = listOf(ImageDto(url = "$ASSET_IMAGE_PATH$image", ratio = "16_9", width = 1024, height = 576)),
            dates = DatesDto(StartDto(localDate = day.toString(), localTime = time.toString(), dateTime = startsAt.toString())),
            classifications = listOf(ClassificationDto(primary = true, segment = NamedDto(segment))),
            priceRanges = minPrice?.let { listOf(PriceRangeDto(currency = "GBP", min = it, max = it)) }.orEmpty(),
            embedded = EmbeddedVenuesDto(
                listOf(
                    VenueDto(
                        name = venue.name,
                        city = NamedDto("Swansea"),
                        address = AddressDto(venue.address),
                        location = LocationDto(venue.latitude.toString(), venue.longitude.toString()),
                    ),
                ),
            ),
        )
    }

    private companion object {
        const val ASSET_IMAGE_PATH = "file:///android_asset/sample_images/"
        const val HTTP_NOT_FOUND = 404
    }
}
