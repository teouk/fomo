package io.github.teouk.fomo.data.local

import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.Venue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun Event.toEntity(isSaved: Boolean, isPlanned: Boolean) = SavedEventEntity(
    id = id,
    name = name,
    localDate = localDate?.toString(),
    localTime = localTime?.toString(),
    startsAtEpochMillis = startsAt?.toEpochMilli(),
    category = category,
    description = description,
    imageUrl = imageUrl,
    ticketUrl = ticketUrl,
    minPrice = minPrice,
    currency = currency,
    venueName = venue?.name,
    venueCity = venue?.city,
    venueAddress = venue?.address,
    venueLatitude = venue?.latitude,
    venueLongitude = venue?.longitude,
    isSaved = isSaved,
    isPlanned = isPlanned,
)

fun SavedEventEntity.toEvent() = Event(
    id = id,
    name = name,
    localDate = localDate?.let(LocalDate::parse),
    localTime = localTime?.let(LocalTime::parse),
    startsAt = startsAtEpochMillis?.let(Instant::ofEpochMilli),
    category = category,
    description = description,
    imageUrl = imageUrl,
    ticketUrl = ticketUrl,
    minPrice = minPrice,
    currency = currency,
    venue = venueName?.let { Venue(it, venueCity, venueAddress, venueLatitude, venueLongitude) },
)
