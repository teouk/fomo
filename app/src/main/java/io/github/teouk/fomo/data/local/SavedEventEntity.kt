package io.github.teouk.fomo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per event the user has saved (bookmarked) or added to their plans, or both.
 * A copy of the event's details is kept so My Plans works offline and needs no API calls.
 *
 * Times are stored as plain values (ISO text and epoch milliseconds) so no Room
 * TypeConverters are needed.
 */
@Entity(tableName = "saved_events")
data class SavedEventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localDate: String?,
    val localTime: String?,
    val startsAtEpochMillis: Long?,
    val category: String?,
    val description: String?,
    val imageUrl: String?,
    val ticketUrl: String?,
    val minPrice: Double?,
    val currency: String?,
    val venueName: String?,
    val venueCity: String?,
    val venueAddress: String?,
    val venueLatitude: Double?,
    val venueLongitude: Double?,
    /** Bookmarked: shown in My Plans → Saved. */
    val isSaved: Boolean,
    /** Added to plans: shown in My Plans → Schedule, with a reminder. */
    val isPlanned: Boolean,
)
