package io.github.teouk.fomo.util

/**
 * Encodes a latitude/longitude pair as a geohash: a short string that names a rectangle
 * on the map. Each extra character makes the rectangle smaller.
 *
 * Ticketmaster's `geoPoint` search parameter takes a geohash, not "lat,long".
 *
 * This is a plain Kotlin `object` (a singleton) with no Android imports, so it can be
 * unit-tested on the computer without an emulator.
 */
object GeoHash {
    /** Ticketmaster accepts at most 9 characters, which is accurate to about 5 metres. */
    const val MAX_PRECISION = 9

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private const val BITS_PER_CHAR = 5

    /**
     * @param precision number of characters in the result, 1 to [MAX_PRECISION]
     * @throws IllegalArgumentException if a value is outside its valid range
     */
    fun encode(latitude: Double, longitude: Double, precision: Int = MAX_PRECISION): String {
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90, was $latitude" }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180, was $longitude" }
        require(precision in 1..MAX_PRECISION) { "precision must be between 1 and $MAX_PRECISION, was $precision" }

        var minLat = -90.0
        var maxLat = 90.0
        var minLon = -180.0
        var maxLon = 180.0

        val hash = StringBuilder(precision)
        var isLongitudeBit = true // bits alternate: longitude, latitude, longitude, ...
        var bitCount = 0
        var charIndex = 0

        while (hash.length < precision) {
            // Halve the current range and record which half the point is in (1 = upper half).
            if (isLongitudeBit) {
                val mid = (minLon + maxLon) / 2
                if (longitude >= mid) {
                    charIndex = charIndex * 2 + 1
                    minLon = mid
                } else {
                    charIndex *= 2
                    maxLon = mid
                }
            } else {
                val mid = (minLat + maxLat) / 2
                if (latitude >= mid) {
                    charIndex = charIndex * 2 + 1
                    minLat = mid
                } else {
                    charIndex *= 2
                    maxLat = mid
                }
            }
            isLongitudeBit = !isLongitudeBit
            bitCount++

            // Every 5 bits make one base-32 character.
            if (bitCount == BITS_PER_CHAR) {
                hash.append(BASE32[charIndex])
                bitCount = 0
                charIndex = 0
            }
        }
        return hash.toString()
    }
}
