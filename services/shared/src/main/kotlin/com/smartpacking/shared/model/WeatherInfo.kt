package com.smartpacking.shared.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Weather information for a destination.
 */
data class WeatherInfo(
    @JsonProperty("temp_min")
    val tempMin: Int,

    @JsonProperty("temp_max")
    val tempMax: Int,

    val conditions: String,

    val humidity: String? = null,

    @JsonProperty("precipitation_chance")
    val precipitationChance: Int? = null
)
