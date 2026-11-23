package io.github.teouk.fomo.model

import java.time.Instant

enum class WeatherCondition {
    CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, SNOW, THUNDERSTORM, UNKNOWN;

    val isWet: Boolean get() = this == DRIZZLE || this == RAIN || this == THUNDERSTORM

    companion object {
        /** Maps a WMO weather code, as used by Open-Meteo, to a condition. */
        fun fromWmoCode(code: Int?): WeatherCondition = when (code) {
            0 -> CLEAR
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            in 51..57 -> DRIZZLE
            in 61..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> THUNDERSTORM
            else -> UNKNOWN
        }
    }
}

data class WeatherForecast(
    val time: Instant,
    val temperatureCelsius: Int,
    val precipitationChancePercent: Int?,
    val condition: WeatherCondition,
)
