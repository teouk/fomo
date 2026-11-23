package io.github.teouk.fomo.data.remote.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * EXTERNAL API: Open-Meteo Forecast API (https://open-meteo.com/en/docs).
 * Used for: the weather card on Event Details (forecast at the venue at the event's hour).
 * Free, no API key; forecasts up to 16 days ahead. Credited on screen as
 * "Forecast by Open-Meteo".
 */
interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun hourlyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("timezone") timezone: String = "UTC",
        @Query("forecast_days") forecastDays: Int = MAX_FORECAST_DAYS,
    ): ForecastResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
        const val MAX_FORECAST_DAYS = 16
    }
}

@Serializable
data class ForecastResponseDto(val hourly: HourlyDto? = null)

/** Parallel lists: index i of every list describes the hour time[i] (UTC, "2026-11-13T19:00"). */
@Serializable
data class HourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
)
