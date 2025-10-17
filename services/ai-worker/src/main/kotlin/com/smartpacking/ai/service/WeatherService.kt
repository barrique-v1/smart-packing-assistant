package com.smartpacking.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.smartpacking.ai.model.WeatherData
import com.smartpacking.ai.model.WeatherDataMap
import com.smartpacking.shared.enums.Season
import com.smartpacking.shared.model.WeatherInfo
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Service for managing weather data.
 * Loads weather data from JSON file on startup and provides methods to retrieve weather information.
 */
@Service
class WeatherService(
    private val objectMapper: ObjectMapper,
    @Value("\${data.weather.location}") private val weatherDataResource: Resource
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)
    private lateinit var weatherDataMap: WeatherDataMap

    /**
     * Loads weather data from JSON file on application startup.
     */
    @PostConstruct
    fun loadWeatherData() {
        try {
            weatherDataResource.inputStream.use { inputStream ->
                weatherDataMap = objectMapper.readValue(inputStream)
                val locationCount = weatherDataMap.keys.size
                val totalEntries = weatherDataMap.values.sumOf { it.size }
                logger.info("Successfully loaded weather data for $locationCount locations with $totalEntries total entries")
            }
        } catch (e: IOException) {
            logger.error("Failed to load weather data from ${weatherDataResource.filename}", e)
            throw IllegalStateException("Could not load weather data", e)
        }
    }

    /**
     * Retrieves weather information for a given location and season.
     *
     * @param location The destination location (case-insensitive)
     * @param season The travel season
     * @return WeatherInfo if found, null otherwise
     */
    fun getWeatherInfo(location: String, season: Season): WeatherInfo? {
        // Normalize location: capitalize first letter of each word for matching
        val normalizedLocation = location.split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }

        val seasonData = weatherDataMap[normalizedLocation]?.get(season.name)

        return if (seasonData != null) {
            WeatherInfo(
                tempMin = seasonData.tempMin,
                tempMax = seasonData.tempMax,
                conditions = seasonData.conditions,
                humidity = seasonData.humidity,
                precipitationChance = seasonData.precipitationChance
            )
        } else {
            logger.debug("No weather data found for location: $normalizedLocation, season: ${season.name}")
            null
        }
    }

    /**
     * Checks if weather data exists for a given location.
     *
     * @param location The destination location (case-insensitive)
     * @return true if weather data exists, false otherwise
     */
    fun hasWeatherData(location: String): Boolean {
        val normalizedLocation = location.split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
        return weatherDataMap.containsKey(normalizedLocation)
    }

    /**
     * Returns all available locations with weather data.
     *
     * @return Set of location names
     */
    fun getAvailableLocations(): Set<String> {
        return weatherDataMap.keys
    }
}
