package io.github.teouk.fomo.data.remote.datathistle

import io.github.teouk.fomo.model.DateWindow
import io.github.teouk.fomo.model.EventFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant

class DataThistleQueryBuilderTest {
    private val now = Instant.parse("2026-11-11T12:00:00.250Z")
    private val swansea = EventFilter(latitude = 51.6214, longitude = -3.9436, radiusMiles = 10)

    @Test
    fun build_defaultFilter_sendsPointRadiusAndFourWeekWindow() {
        val query = DataThistleQueryBuilder.build(swansea, now)
        assertEquals("51.6214", query["lat"])
        assertEquals("-3.9436", query["lon"])
        assertEquals("10", query["distance"])
        assertEquals("2026-11-11T12:00:00Z", query["min_date"])
        assertEquals("2026-12-09T12:00:00Z", query["max_date"])
        assertEquals("20", query["limit"])
        assertFalse(query.containsKey("name"))
    }

    @Test
    fun build_dateWindowInFuture_usesWindow() {
        val window = DateWindow(Instant.parse("2026-11-14T00:00:00Z"), Instant.parse("2026-11-16T00:00:00Z"))
        val query = DataThistleQueryBuilder.build(swansea.copy(dateWindow = window), now)
        assertEquals("2026-11-14T00:00:00Z", query["min_date"])
        assertEquals("2026-11-16T00:00:00Z", query["max_date"])
    }

    @Test
    fun build_windowStartedInPast_startsNow() {
        val window = DateWindow(Instant.parse("2026-11-11T00:00:00Z"), Instant.parse("2026-11-12T00:00:00Z"))
        val query = DataThistleQueryBuilder.build(swansea.copy(dateWindow = window), now)
        assertEquals("2026-11-11T12:00:00Z", query["min_date"])
    }

    @Test
    fun build_keyword_isSentAsTrimmedName() {
        assertEquals("harbour", DataThistleQueryBuilder.build(swansea.copy(keyword = " harbour "), now)["name"])
    }
}
