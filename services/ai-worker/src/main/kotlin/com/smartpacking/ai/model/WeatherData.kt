package com.smartpacking.ai.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data class representing weather information for a specific location and season.
 * This mirrors the structure in weather_data.json.
 */
data class WeatherData(
    @JsonProperty("temp_min")
    val tempMin: Int,

    @JsonProperty("temp_max")
    val tempMax: Int,

    @JsonProperty("conditions")
    val conditions: String,

    @JsonProperty("humidity")
    val humidity: String,

    @JsonProperty("precipitation_chance")
    val precipitationChance: Int
)

/**
 * Container for all weather data organized by location and season.
 * Key = Location name (e.g., "Iceland", "Dubai", "Tokyo")
 * Value = Map of Season to WeatherData
 */
typealias WeatherDataMap = Map<String, Map<String, WeatherData>>
