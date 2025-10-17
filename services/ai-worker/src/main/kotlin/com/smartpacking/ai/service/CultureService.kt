package com.smartpacking.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.smartpacking.ai.model.CultureTip
import com.smartpacking.ai.model.CultureTipsMap
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Service for managing culture tips.
 * Loads culture tips from JSON file on startup and provides methods to retrieve tips.
 */
@Service
class CultureService(
    private val objectMapper: ObjectMapper,
    @Value("\${data.culture.location}") private val cultureTipsResource: Resource
) {
    private val logger = LoggerFactory.getLogger(CultureService::class.java)
    private lateinit var cultureTipsMap: CultureTipsMap

    /**
     * Loads culture tips from JSON file on application startup.
     */
    @PostConstruct
    fun loadCultureTips() {
        try {
            cultureTipsResource.inputStream.use { inputStream ->
                cultureTipsMap = objectMapper.readValue(inputStream)
                val locationCount = cultureTipsMap.keys.size
                val totalTips = cultureTipsMap.values.sumOf { it.size }
                logger.info("Successfully loaded culture tips for $locationCount locations with $totalTips total tips")
            }
        } catch (e: IOException) {
            logger.error("Failed to load culture tips from ${cultureTipsResource.filename}", e)
            throw IllegalStateException("Could not load culture tips", e)
        }
    }

    /**
     * Retrieves all culture tips for a given location.
     *
     * @param location The destination location (case-insensitive)
     * @return List of CultureTip if found, empty list otherwise
     */
    fun getCultureTips(location: String): List<CultureTip> {
        // Normalize location: capitalize first letter of each word for matching
        val normalizedLocation = location.split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }

        val tips = cultureTipsMap[normalizedLocation] ?: emptyList()

        if (tips.isEmpty()) {
            logger.debug("No culture tips found for location: $normalizedLocation")
        }

        return tips
    }

    /**
     * Retrieves culture tips filtered by importance level.
     *
     * @param location The destination location (case-insensitive)
     * @param minImportance Minimum importance level (LOW, MEDIUM, HIGH)
     * @return List of CultureTip matching the criteria
     */
    fun getCultureTipsByImportance(location: String, minImportance: String): List<CultureTip> {
        val tips = getCultureTips(location)

        val importanceLevels = listOf("LOW", "MEDIUM", "HIGH")
        val minLevel = importanceLevels.indexOf(minImportance.uppercase())

        if (minLevel == -1) {
            logger.warn("Invalid importance level: $minImportance. Returning all tips.")
            return tips
        }

        return tips.filter { tip ->
            val tipLevel = importanceLevels.indexOf(tip.importance.uppercase())
            tipLevel >= minLevel
        }
    }

    /**
     * Retrieves culture tips filtered by category.
     *
     * @param location The destination location (case-insensitive)
     * @param category The category to filter by (DRESS_CODE, CUSTOMS, LANGUAGE, SAFETY)
     * @return List of CultureTip matching the category
     */
    fun getCultureTipsByCategory(location: String, category: String): List<CultureTip> {
        val tips = getCultureTips(location)
        return tips.filter { it.category.equals(category, ignoreCase = true) }
    }

    /**
     * Checks if culture tips exist for a given location.
     *
     * @param location The destination location (case-insensitive)
     * @return true if culture tips exist, false otherwise
     */
    fun hasCultureTips(location: String): Boolean {
        val normalizedLocation = location.split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
        return cultureTipsMap.containsKey(normalizedLocation)
    }

    /**
     * Returns all available locations with culture tips.
     *
     * @return Set of location names
     */
    fun getAvailableLocations(): Set<String> {
        return cultureTipsMap.keys
    }
}
