package io.github.teouk.fomo.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * An event as the app uses it. This is the "domain model": the UI and the database
 * work with this class and never see Ticketmaster's JSON shape.
 *
 * Fields that Ticketmaster often leaves out are nullable, so the compiler forces every
 * screen to decide what to show when they are missing (for example "Price TBC").
 */
data class Event(
    val id: String,
    val name: String,
    /** Date and time as printed on the ticket, in the venue's own time zone. Used for display. */
    val localDate: LocalDate?,
    val localTime: LocalTime?,
    /** The same moment in UTC. Used for logic such as reminders and clash checks. */
    val startsAt: Instant?,
    val category: String?,
    val description: String?,
    val imageUrl: String?,
    val ticketUrl: String?,
    val minPrice: Double?,
    val currency: String?,
    val venue: Venue?,
    /**
     * Distance from the searched location in miles, when the API reports it.
     * Used when the venue has no coordinates to calculate it from.
     */
    val distanceMiles: Double? = null,
)

data class Venue(
    val name: String,
    val city: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)
