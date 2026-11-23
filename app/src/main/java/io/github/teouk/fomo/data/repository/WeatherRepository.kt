package io.github.teouk.fomo.data.repository

import io.github.teouk.fomo.data.remote.weather.ForecastResponseDto
import io.github.teouk.fomo.data.remote.weather.OpenMeteoService
import io.github.teouk.fomo.model.WeatherCondition
import io.github.teouk.fomo.model.WeatherForecast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class WeatherRepository(
    private val service: OpenMeteoService,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * The forecast at [latitude],[longitude] for the hour of [at], or null when there is
     * none: the event is in the past, more than 16 days away, or the API is unreachable.
     * Weather is a nice extra, so failures hide the card instead of showing an error.
     */
    suspend fun forecastFor(latitude: Double, longitude: Double, at: Instant): WeatherForecast? {
        val now = clock.instant()
        if (at.isBefore(now.minus(Duration.ofHours(1)))) return null
        if (at.isAfter(now.plus(Duration.ofDays(OpenMeteoService.MAX_FORECAST_DAYS.toLong())))) return null

        return withContext(ioDispatcher) {
            try {
                pickHour(service.hourlyForecast(latitude, longitude), at)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                null
            } catch (e: retrofit2.HttpException) {
                null
            } catch (e: kotlinx.serialization.SerializationException) {
                null
            }
        }
    }

    companion object {
        /** Finds the hourly entry that contains [at]. Pure function, so it is unit-tested. */
        fun pickHour(response: ForecastResponseDto, at: Instant): WeatherForecast? {
            val hourly = response.hourly ?: return null
            val hour = at.truncatedTo(ChronoUnit.HOURS)
            val index = hourly.time.indexOfFirst { LocalDateTime.parse(it).toInstant(ZoneOffset.UTC) == hour }
            if (index < 0) return null
            val temperature = hourly.temperature.getOrNull(index) ?: return null
            return WeatherForecast(
                time = at,
                temperatureCelsius = temperature.roundToInt(),
                precipitationChancePercent = hourly.precipitationProbability.getOrNull(index),
                condition = WeatherCondition.fromWmoCode(hourly.weatherCode.getOrNull(index)),
            )
        }
    }
}
