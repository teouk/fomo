package io.github.teouk.fomo.ui.common

import io.github.teouk.fomo.data.calendar.Clash
import io.github.teouk.fomo.model.Event

/** An event plus what one list row needs to show about it. */
data class EventListItem(
    val event: Event,
    /** Miles from the chosen location, or null if the venue has no coordinates. */
    val distanceMiles: Double?,
    val isSaved: Boolean,
    /** Set when Calendar Clash Check finds an overlapping calendar entry. */
    val clash: Clash? = null,
)
