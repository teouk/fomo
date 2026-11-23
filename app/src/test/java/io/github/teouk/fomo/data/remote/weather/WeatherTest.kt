package io.github.teouk.fomo.data.remote.weather

import io.github.teouk.fomo.data.repository.WeatherRepository
import io.github.teouk.fomo.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class WeatherTest {
    private val response = ForecastResponseDto(
        HourlyDto(
            time = listOf("2026-11-13T18:00", "2026-11-13T19:00", "2026-11-13T20:00"),
            temperature = listOf(9.4, 8.6, 7.9),
            precipitationProbability = listOf(40, 70, 55),
            weatherCode = listOf(3, 61, 80),
        ),
    )

    @Test
    fun fromWmoCode_knownCodes_mapToConditions() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromWmoCode(3))
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromWmoCode(48))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCondition.fromWmoCode(53))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(63))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(81))
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmoCode(75))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95))
    }

    @Test
    fun fromWmoCode_unknownOrMissing_isUnknown() {
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWmoCode(42))
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWmoCode(null))
    }

    @Test
    fun pickHour_eventMidHour_usesTheHourItFallsIn() {
        val forecast = WeatherRepository.pickHour(response, Instant.parse("2026-11-13T19:30:00Z"))!!
        assertEquals(9, forecast.temperatureCelsius)
        assertEquals(70, forecast.precipitationChancePercent)
        assertEquals(WeatherCondition.RAIN, forecast.condition)
    }

    @Test
    fun pickHour_hourNotInForecast_returnsNull() {
        assertNull(WeatherRepository.pickHour(response, Instant.parse("2026-11-14T19:00:00Z")))
    }
}
