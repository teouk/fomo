package io.github.teouk.fomo.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterLineCleanerTest {
    @Test
    fun clean_typicalPoster_keepsNamesAndDates() {
        val lines = listOf("THE HARBOUR LIGHTS", "LIVE AT", "BRANGWYN HALL", "FRI 13 NOV", "£15", "www.example.co.uk")
        assertEquals(listOf("THE HARBOUR LIGHTS", "LIVE AT", "BRANGWYN HALL", "FRI 13 NOV"), PosterLineCleaner.clean(lines))
    }

    @Test
    fun clean_titleSplitOverTwoLines_isJoined() {
        assertEquals(
            listOf("LIVE MUSIC", "THE NUTCRACKER", "SWANSEA GRAND THEATRE"),
            PosterLineCleaner.clean(listOf("LIVE MUSIC", "THE", "NUTCRACKER", "SWANSEA GRAND THEATRE")),
        )
    }

    @Test
    fun clean_pricesPhoneNumbersAndEmails_areDropped() {
        val lines = listOf("£12.50", "01792 123456", "tickets@venue.com", "19:30", "Doors 7pm")
        assertEquals(listOf("Doors 7pm"), PosterLineCleaner.clean(lines))
    }

    @Test
    fun clean_shortOrDecorativeLines_areDropped() {
        assertEquals(emptyList<String>(), PosterLineCleaner.clean(listOf("A", "~*~", "--", "x2")))
    }

    @Test
    fun clean_duplicatesInDifferentCase_keepFirst() {
        assertEquals(listOf("Jazz Night"), PosterLineCleaner.clean(listOf("Jazz Night", "JAZZ NIGHT", "  jazz   night ")))
    }

    @Test
    fun clean_manyLines_areCapped() {
        val lines = (1..20).map { "Line number ${'a' + it}" }
        assertEquals(PosterLineCleaner.MAX_SUGGESTIONS, PosterLineCleaner.clean(lines).size)
    }
}
