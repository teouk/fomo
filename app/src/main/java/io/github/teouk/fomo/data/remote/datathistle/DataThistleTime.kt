package io.github.teouk.fomo.data.remote.datathistle

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Data Thistle's examples use three timestamp shapes:
 *  "2025-04-01T10:30:00+01:00"  (ISO with offset)
 *  "2025-04-10 18:45:00+01"     (space instead of T, hour-only offset)
 *  "2025-03-30T09:30:00"        (no offset: UK local time)
 * This parser accepts all three and returns null for anything else.
 */
object DataThistleTime {
    private val UK = ZoneId.of("Europe/London")
    private val spacedWithShortOffset = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]X")

    fun parse(text: String?): Instant? {
        if (text.isNullOrBlank()) return null
        val value = text.trim()
        return tryParse { OffsetDateTime.parse(value).toInstant() }
            ?: tryParse { OffsetDateTime.parse(value, spacedWithShortOffset).toInstant() }
            ?: tryParse { LocalDateTime.parse(value.replace(' ', 'T')).atZone(UK).toInstant() }
            ?: tryParse { Instant.parse(value) }
    }

    private inline fun tryParse(block: () -> Instant): Instant? = try {
        block()
    } catch (e: DateTimeParseException) {
        null
    }
}
