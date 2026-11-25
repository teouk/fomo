package io.github.teouk.fomo.data.remote.ticketmaster

import io.github.teouk.fomo.model.Category
import io.github.teouk.fomo.model.DateWindow
import io.github.teouk.fomo.model.EventFilter
import io.github.teouk.fomo.util.GeoHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class EventQueryBuilderTest {
    private val swansea = EventFilter(latitude = 51.6214, longitude = -3.9436)

    @Test
    fun build_defaultFilter_sendsGeohashRadiusInMilesAndSoonestFirst() {
        val query = EventQueryBuilder.build(swansea)
        assertEquals(GeoHash.encode(51.6214, -3.9436), query["geoPoint"])
        assertEquals("25", query["radius"])
        assertEquals("miles", query["unit"])
        assertEquals("date,asc", query["sort"])
    }

    @Test
    fun build_defaultFilter_omitsOptionalParameters() {
        val query = EventQueryBuilder.build(swansea)
        assertFalse(query.containsKey("keyword"))
        assertFalse(query.containsKey("classificationName"))
        assertFalse(query.containsKey("startDateTime"))
        assertFalse(query.containsKey("endDateTime"))
    }

    @Test
    fun build_keywordWithSpaces_isTrimmed() {
        val query = EventQueryBuilder.build(swansea.copy(keyword = "  harbour lights "))
        assertEquals("harbour lights", query["keyword"])
    }

    @Test
    fun build_blankKeyword_isOmitted() {
        assertFalse(EventQueryBuilder.build(swansea.copy(keyword = "   ")).containsKey("keyword"))
    }

    @Test
    fun build_severalCategories_joinsNamesInStableOrder() {
        val first = EventQueryBuilder.build(swansea.copy(categories = setOf(Category.SPORT, Category.MUSIC)))
        val second = EventQueryBuilder.build(swansea.copy(categories = setOf(Category.MUSIC, Category.SPORT)))
        assertEquals("Music,Sports", first["classificationName"])
        assertEquals(first, second)
    }

    @Test
    fun build_dateWindow_formatsUtcWithoutMilliseconds() {
        val window = DateWindow(Instant.parse("2026-11-13T18:00:00.750Z"), Instant.parse("2026-11-14T00:00:00Z"))
        val query = EventQueryBuilder.build(swansea.copy(dateWindow = window))
        assertEquals("2026-11-13T18:00:00Z", query["startDateTime"])
        assertEquals("2026-11-14T00:00:00Z", query["endDateTime"])
    }

    @Test
    fun build_pageAndSize_areSent() {
        val query = EventQueryBuilder.build(swansea, page = 2, pageSize = 20)
        assertEquals("2", query["page"])
        assertEquals("20", query["size"])
    }

    @Test
    fun build_pagingPastTicketmasterLimit_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            EventQueryBuilder.build(swansea, page = 20, pageSize = 50)
        }
    }
}
