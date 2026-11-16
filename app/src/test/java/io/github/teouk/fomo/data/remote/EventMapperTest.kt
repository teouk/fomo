package io.github.teouk.fomo.data.remote

import io.github.teouk.fomo.data.remote.dto.EventSearchResponseDto
import io.github.teouk.fomo.model.Event
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Parses a saved response and maps it, so the DTOs and the mapper are tested together.
 *
 * NOTE: ticketmaster_search_sample.json is hand-written from the Discovery API v2
 * documentation. It will be replaced by a real saved response once the API key is active.
 */
class EventMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val events: List<Event> by lazy {
        val text = checkNotNull(javaClass.getResource("/ticketmaster_search_sample.json")).readText()
        val response = json.decodeFromString<EventSearchResponseDto>(text)
        response.embedded!!.events.map { it.toEvent() }
    }

    private val fullEvent get() = events[0]
    private val sparseEvent get() = events[1]

    @Test
    fun toEvent_fullEvent_mapsNameIdAndTicketUrl() {
        assertEquals("G5vYZ9sample01", fullEvent.id)
        assertEquals("The Harbour Lights", fullEvent.name)
        assertEquals(
            "https://www.ticketmaster.co.uk/the-harbour-lights-swansea/event/sample01",
            fullEvent.ticketUrl,
        )
    }

    @Test
    fun toEvent_fullEvent_parsesLocalAndUtcStart() {
        assertEquals(LocalDate.of(2026, 11, 13), fullEvent.localDate)
        assertEquals(LocalTime.of(19, 30), fullEvent.localTime)
        assertEquals(Instant.parse("2026-11-13T19:30:00Z"), fullEvent.startsAt)
    }

    @Test
    fun toEvent_severalPriceRanges_usesCheapest() {
        assertEquals(27.5, fullEvent.minPrice!!, 0.001)
        assertEquals("GBP", fullEvent.currency)
    }

    @Test
    fun toEvent_severalImages_picksSmallest16by9AtLeast1024Wide() {
        assertEquals("https://example.com/harbour_16_9_medium.jpg", fullEvent.imageUrl)
    }

    @Test
    fun toEvent_fullEvent_mapsVenueWithNumericCoordinates() {
        val venue = assertNotNullAndReturn(fullEvent.venue)
        assertEquals("Swansea Arena", venue.name)
        assertEquals("Swansea", venue.city)
        assertEquals("Oystermouth Road", venue.address)
        assertEquals(51.6144, venue.latitude!!, 0.0001)
        assertEquals(-3.9436, venue.longitude!!, 0.0001)
    }

    @Test
    fun toEvent_primaryClassification_usesSegmentName() {
        assertEquals("Music", fullEvent.category)
    }

    @Test
    fun toEvent_infoButNoDescription_usesInfo() {
        assertEquals("Doors open at 18:30.", fullEvent.description)
    }

    @Test
    fun toEvent_missingOptionalFields_mapsToNull() {
        assertEquals(LocalDate.of(2026, 11, 20), sparseEvent.localDate)
        assertNull(sparseEvent.localTime)
        assertNull(sparseEvent.startsAt)
        assertNull(sparseEvent.minPrice)
        assertNull(sparseEvent.imageUrl)
        assertNull(sparseEvent.venue)
        assertNull(sparseEvent.ticketUrl)
    }

    @Test
    fun toEvent_undefinedClassification_mapsCategoryToNull() {
        assertNull(sparseEvent.category)
    }

    private fun <T> assertNotNullAndReturn(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
