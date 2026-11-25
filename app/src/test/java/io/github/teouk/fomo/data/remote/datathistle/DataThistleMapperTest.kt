package io.github.teouk.fomo.data.remote.datathistle

import io.github.teouk.fomo.model.Event
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * NOTE: datathistle_events_sample.json is written from the examples in Data Thistle's
 * OpenAPI spec, not saved from a live call. Replace it with a real response once the
 * access token works, and re-run these tests.
 */
class DataThistleMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val dtos: List<DtEventDto> by lazy {
        val text = checkNotNull(javaClass.getResource("/datathistle_events_sample.json")).readText()
        json.decodeFromString<List<DtEventDto>>(text)
    }

    private fun mapAt(now: String, index: Int): Event = dtos[index].toEvent(Instant.parse(now))

    @Test
    fun toEvent_beforeFirstPerformance_usesFirstPerformance() {
        val event = mapAt("2026-11-11T12:00:00Z", 0)
        assertEquals(Instant.parse("2026-11-13T19:30:00Z"), event.startsAt)
        assertEquals(LocalDate.of(2026, 11, 13), event.localDate)
        assertEquals(LocalTime.of(19, 30), event.localTime)
        assertEquals("https://www.datathistle.com/details/abc/book", event.ticketUrl)
    }

    @Test
    fun toEvent_afterFirstPerformance_usesNextOne() {
        val event = mapAt("2026-11-14T12:00:00Z", 0)
        assertEquals(Instant.parse("2026-11-20T19:30:00Z"), event.startsAt)
        assertEquals("https://www.datathistle.com/details/def/book", event.ticketUrl)
    }

    @Test
    fun toEvent_severalTickets_usesCheapestPrice() {
        val event = mapAt("2026-11-11T12:00:00Z", 0)
        assertEquals(18.5, event.minPrice!!, 0.001)
        assertEquals("GBP", event.currency)
    }

    @Test
    fun toEvent_fullEvent_mapsVenueImageDescriptionAndDistance() {
        val event = mapAt("2026-11-11T12:00:00Z", 0)
        assertEquals("Swansea Arena", event.venue?.name)
        assertEquals("Swansea", event.venue?.city)
        assertEquals(51.6144, event.venue?.latitude!!, 0.0001)
        assertEquals("https://files.datathistle.com/images/large.jpg", event.imageUrl)
        assertEquals("Live jazz on the waterfront with the Bay Big Band.", event.description)
        assertEquals("Music", event.category)
        assertEquals(0.42, event.distanceMiles!!, 0.001)
    }

    @Test
    fun toEvent_noPerformances_fallsBackToScheduleStartInUkTime() {
        val event = mapAt("2026-11-11T12:00:00Z", 1)
        // 19:00 UK time in November (GMT) is 19:00 UTC.
        assertEquals(Instant.parse("2026-11-15T19:00:00Z"), event.startsAt)
        assertNull(event.minPrice)
        assertNull(event.imageUrl)
        assertNull(event.ticketUrl)
        assertNull(event.category)
        assertNull(event.venue?.latitude)
    }
}
