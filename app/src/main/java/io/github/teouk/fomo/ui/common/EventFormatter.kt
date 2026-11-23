package io.github.teouk.fomo.ui.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** How an event's price should be shown. The UI turns each case into a string resource. */
sealed interface PriceLabel {
    data object Free : PriceLabel

    data class From(val amount: String) : PriceLabel

    data object ToBeConfirmed : PriceLabel
}

/**
 * Turns event values into display text. Pure Kotlin with a fixed UK locale, so it is
 * unit-tested; screens only add the surrounding words from strings.xml.
 */
object EventFormatter {
    private val locale = Locale.UK
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    private val longDayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", locale)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", locale)

    /** "Fri 13 Nov · 19:30", or just the day when the time is not announced yet. */
    fun dateTime(date: LocalDate?, time: LocalTime?): String? {
        if (date == null) return null
        val day = date.format(dayFormat)
        return if (time == null) day else "$day · ${time.format(timeFormat)}"
    }

    /** "Friday 13 November", used as a heading in the schedule. */
    fun longDay(date: LocalDate): String = date.format(longDayFormat)

    fun time(time: LocalTime): String = time.format(timeFormat)

    fun price(minPrice: Double?, currencyCode: String?): PriceLabel {
        if (minPrice == null) return PriceLabel.ToBeConfirmed
        if (minPrice == 0.0) return PriceLabel.Free
        val format = NumberFormat.getCurrencyInstance(locale)
        currencyCode?.let { code -> runCatching { Currency.getInstance(code) }.getOrNull()?.let { format.currency = it } }
        // Whole amounts drop the pence: £15 rather than £15.00.
        if (minPrice % 1.0 == 0.0) format.maximumFractionDigits = 0
        return PriceLabel.From(format.format(minPrice))
    }

    /** "0.8 mi" below 10 miles, "12 mi" above. */
    fun distance(miles: Double): String =
        if (miles < 10) String.format(locale, "%.1f mi", miles) else String.format(locale, "%.0f mi", miles)
}
