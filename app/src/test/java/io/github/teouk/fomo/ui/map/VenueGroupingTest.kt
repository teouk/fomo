package io.github.teouk.fomo.ui.map

import io.github.teouk.fomo.data.repository.EventStatus
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.Venue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VenueGroupingTest {
    private val cinema = Venue("Vue Swansea", "Swansea", null, 51.6206, -3.9400)
    private val theatre = Venue("Volcano Theatre", "Swansea", null, 51.6232, -3.9423)
    private val noCoordinates = Venue("Somewhere", "Swansea", null, null, null)

    private fun event(id: String, venue: Venue?) = Event(
        id = id, name = id, localDate = null, localTime = null, startsAt = null, category = null,
        description = null, imageUrl = null, ticketUrl = null, minPrice = null, currency = null, venue = venue,
    )

    @Test
    fun groupByVenue_severalEventsAtOneVenue_makeOneMarker() {
        val markers = MapViewModel.groupByVenue(
            listOf(event("a", cinema), event("b", cinema), event("c", theatre)),
            emptyMap(),
        )
        assertEquals(2, markers.size)
        assertEquals(listOf("a", "b"), markers.first { it.name == "Vue Swansea" }.events.map { it.event.id })
    }

    @Test
    fun groupByVenue_venueWithoutCoordinates_isLeftOff() {
        val markers = MapViewModel.groupByVenue(listOf(event("a", noCoordinates), event("b", null)), emptyMap())
        assertTrue(markers.isEmpty())
    }

    @Test
    fun groupByVenue_savedEvent_isMarkedSaved() {
        val markers = MapViewModel.groupByVenue(listOf(event("a", cinema)), mapOf("a" to EventStatus(isSaved = true)))
        assertTrue(markers.single().events.single().isSaved)
    }
}
