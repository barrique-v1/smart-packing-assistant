package com.smartpacking.ai.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

/**
 * Service for validating destination names against a whitelist.
 *
 * This service prevents hallucinations by ensuring only valid, pre-approved
 * destinations are accepted before AI processing begins.
 *
 * Features:
 * - Case-insensitive validation
 * - Whitespace normalization
 * - Fuzzy suggestion matching
 * - Loaded from application.properties on startup
 */
@Service
class DestinationValidationService(
    @Value("\${valid.destinations}") private val destinationsConfig: String
) {
    private val logger = LoggerFactory.getLogger(DestinationValidationService::class.java)

    private val validDestinations = mutableSetOf<String>()

    @PostConstruct
    fun init() {
        // Parse comma-separated list and normalize
        validDestinations.addAll(
            destinationsConfig
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.lowercase() }
        )

        logger.info("✓ Loaded ${validDestinations.size} valid destinations")
        logger.debug("Valid destinations: ${validDestinations.take(10).joinToString(", ")}...")
    }

    /**
     * Validate if destination is in the whitelist.
     *
     * Performs case-insensitive matching after trimming whitespace.
     *
     * @param destination User input (city or country name)
     * @return true if valid, false otherwise
     */
    fun validateDestination(destination: String): Boolean {
        val normalized = destination.trim().lowercase()

        if (normalized.isBlank()) {
            logger.warn("Empty destination provided")
            return false
        }

        val isValid = validDestinations.contains(normalized)

        if (isValid) {
            logger.info("✓ Valid destination: $destination")
        } else {
            logger.warn("✗ Invalid destination: $destination")
        }

        return isValid
    }

    /**
     * Get all valid destinations.
     *
     * Returns the complete whitelist sorted alphabetically.
     * Useful for API responses and frontend autocomplete.
     *
     * @return Sorted list of valid destinations
     */
    fun getAllDestinations(): List<String> {
        return validDestinations.sorted()
    }

    /**
     * Get suggested destinations based on partial match.
     *
     * Performs fuzzy matching by checking if the input is contained
     * in any destination name or vice versa.
     *
     * Examples:
     * - "tok" → ["tokyo"]
     * - "new" → ["new york", "new orleans", "new zealand"]
     * - "united" → ["united states", "united kingdom", "united arab emirates"]
     *
     * @param input Partial destination name (minimum 2 characters)
     * @return List of matching destinations (max 10), sorted alphabetically
     */
    fun getSuggestedDestinations(input: String): List<String> {
        if (input.length < 2) return emptyList()

        val normalized = input.trim().lowercase()

        val suggestions = validDestinations
            .filter { destination ->
                // Match if input is contained in destination OR destination is contained in input
                destination.contains(normalized) || normalized.contains(destination)
            }
            .take(10)
            .sorted()

        if (suggestions.isNotEmpty()) {
            logger.debug("Found ${suggestions.size} suggestions for input: $input")
        }

        return suggestions
    }

    /**
     * Check if destination whitelist is empty.
     *
     * Used for health checks to ensure configuration loaded properly.
     *
     * @return true if whitelist has destinations
     */
    fun hasDestinations(): Boolean {
        return validDestinations.isNotEmpty()
    }

    /**
     * Get whitelist statistics.
     *
     * Useful for monitoring and debugging.
     *
     * @return Map with statistics (count, sample destinations)
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "totalDestinations" to validDestinations.size,
            "sampleDestinations" to validDestinations.take(5).sorted(),
            "configurationLoaded" to hasDestinations()
        )
    }
}
