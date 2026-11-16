package io.github.teouk.fomo.model

/**
 * Everything the user can choose when looking for events. Discover and Search both
 * describe what they want with this one class, and the data layer turns it into a request.
 */
data class EventFilter(
    val latitude: Double,
    val longitude: Double,
    val radiusMiles: Int = DEFAULT_RADIUS_MILES,
    val keyword: String = "",
    /** Empty means every category. */
    val categories: Set<Category> = emptySet(),
    /** Null means any date from now on. */
    val dateWindow: DateWindow? = null,
) {
    companion object {
        const val MIN_RADIUS_MILES = 5
        const val MAX_RADIUS_MILES = 50
        const val DEFAULT_RADIUS_MILES = 25
    }
}
