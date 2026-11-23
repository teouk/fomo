package io.github.teouk.fomo.ui.plans

import io.github.teouk.fomo.model.Event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PlansGroupingTest {
    private fun event(id: String, date: LocalDate?) = Event(
        id = id, name = id, localDate = date, localTime = null, startsAt = null, category = null,
        description = null, imageUrl = null, ticketUrl = null, minPrice = null, currency = null, venue = null,
    )

    @Test
    fun groupByDay_twoEventsSameDay_shareOneHeader() {
        val day = LocalDate.of(2026, 11, 13)
        val rows = PlansViewModel.groupByDay(listOf(event("a", day), event("b", day)), emptyMap())
        assertEquals(3, rows.size)
        assertEquals(PlansRow.DayHeader(day), rows[0])
    }

    @Test
    fun groupByDay_differentDays_eachGetsAHeader() {
        val rows = PlansViewModel.groupByDay(
            listOf(event("a", LocalDate.of(2026, 11, 13)), event("b", LocalDate.of(2026, 11, 14))),
            emptyMap(),
        )
        assertEquals(2, rows.count { it is PlansRow.DayHeader })
    }
}
