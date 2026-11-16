package io.github.teouk.fomo.model

/**
 * The event categories shown as chips on Discover and Search.
 *
 * @property classificationName the value sent to Ticketmaster's `classificationName`
 * parameter. TODO(api-key): confirm each value returns results with a real request;
 * Comedy and Family may be genres rather than top-level segments.
 */
enum class Category(val classificationName: String) {
    MUSIC("Music"),
    SPORT("Sports"),
    ARTS_THEATRE("Arts & Theatre"),
    COMEDY("Comedy"),
    FAMILY("Family"),
    FILM("Film"),
}
