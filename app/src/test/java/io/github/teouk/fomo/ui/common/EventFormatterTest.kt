package io.github.teouk.fomo.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventFormatterTest {
    @Test
    fun dateTime_dateAndTime_joinsWithDot() {
        assertEquals("Fri 13 Nov · 19:30", EventFormatter.dateTime(LocalDate.of(2026, 11, 13), LocalTime.of(19, 30)))
    }

    @Test
    fun dateTime_noTime_showsDayOnly() {
        assertEquals("Fri 13 Nov", EventFormatter.dateTime(LocalDate.of(2026, 11, 13), null))
    }

    @Test
    fun dateTime_noDate_returnsNull() {
        assertNull(EventFormatter.dateTime(null, LocalTime.NOON))
    }

    @Test
    fun price_wholePounds_dropsPence() {
        assertEquals(PriceLabel.From("£15"), EventFormatter.price(15.0, "GBP"))
    }

    @Test
    fun price_withPence_keepsTwoDecimals() {
        assertEquals(PriceLabel.From("£27.50"), EventFormatter.price(27.5, "GBP"))
    }

    @Test
    fun price_otherCurrency_usesItsSymbol() {
        assertEquals(PriceLabel.From("€20"), EventFormatter.price(20.0, "EUR"))
    }

    @Test
    fun price_zero_isFree() {
        assertEquals(PriceLabel.Free, EventFormatter.price(0.0, "GBP"))
    }

    @Test
    fun price_missing_isToBeConfirmed() {
        assertEquals(PriceLabel.ToBeConfirmed, EventFormatter.price(null, null))
    }

    @Test
    fun distance_underTenMiles_showsOneDecimal() {
        assertEquals("0.8 mi", EventFormatter.distance(0.84))
    }

    @Test
    fun distance_tenMilesOrMore_roundsToWholeMiles() {
        assertEquals("12 mi", EventFormatter.distance(12.4))
    }
}
