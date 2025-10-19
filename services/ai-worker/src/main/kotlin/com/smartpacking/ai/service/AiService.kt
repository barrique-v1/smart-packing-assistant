package com.smartpacking.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.smartpacking.ai.exception.*
import com.smartpacking.ai.model.AiPackingResponse
import com.smartpacking.ai.model.PackingCategories
import com.smartpacking.ai.model.PackingItem
import com.smartpacking.shared.enums.Season
import com.smartpacking.shared.enums.TravelType
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeoutException
import kotlin.system.measureTimeMillis

/**
 * Core AI service for generating packing lists using Spring AI and OpenAI.
 *
 * This service integrates PromptService with Spring AI's ChatClient to:
 * - Generate context-aware packing list recommendations
 * - Parse and validate JSON responses
 * - Handle errors with fallback strategies
 * - Log all interactions for debugging and quality assurance
 * - Track performance metrics
 *
 */
@Service
class AiService(
    private val chatClient: ChatClient,
    private val promptService: PromptService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AiService::class.java)

    /**
     * Generates a packing list using OpenAI based on trip parameters.
     *
     * This method:
     * 1. Builds a comprehensive prompt using PromptService
     * 2. Sends the prompt to OpenAI via ChatClient
     * 3. Parses and validates the JSON response
     * 4. Handles errors with fallback to dummy data if necessary
     * 5. Logs the complete interaction for debugging
     *
     * @param destination Travel destination (e.g., "Dubai", "Iceland")
     * @param durationDays Trip duration in days
     * @param season Season of travel
     * @param travelType Type of travel (BUSINESS, VACATION, BACKPACKING)
     * @param useFallbackOnError If true, returns dummy data on error; if false, throws exception
     * @return Parsed and validated packing list response
     * @throws AiServiceException if API call fails and useFallbackOnError is false
     */
    fun generatePackingList(
        destination: String,
        durationDays: Int,
        season: Season,
        travelType: TravelType,
        useFallbackOnError: Boolean = true
    ): AiPackingResponse {
        logger.info("=== Starting packing list generation ===")
        logger.info("Destination: $destination, Duration: $durationDays days, Season: $season, Type: $travelType")

        try {
            // Step 1: Build the prompt with full context
            val userPrompt = promptService.buildPackingListPrompt(
                destination = destination,
                durationDays = durationDays,
                season = season,
                travelType = travelType
            )

            val systemPrompt = promptService.getSystemPrompt()

            logger.debug("System Prompt length: ${systemPrompt.length} characters")
            logger.debug("User Prompt length: ${userPrompt.length} characters")

            // Log the full prompts for debugging (can be disabled in production)
            if (logger.isTraceEnabled) {
                logger.trace("=== SYSTEM PROMPT ===")
                logger.trace(systemPrompt)
                logger.trace("=== USER PROMPT ===")
                logger.trace(userPrompt)
            }

            // Step 2: Call OpenAI API and measure response time
            var rawResponse: String
            val generationTimeMs = measureTimeMillis {
                rawResponse = callOpenAiApi(systemPrompt, userPrompt)
            }

            // Step 3: Log the response and metrics
            logger.info("Generation completed in ${generationTimeMs}ms")
            logger.info("Response length: ${rawResponse.length} characters")

            // Log first 200 characters of response for quick verification
            val preview = if (rawResponse.length > 200) {
                "${rawResponse.take(200)}..."
            } else {
                rawResponse
            }
            logger.info("Response preview: $preview")

            // Log full response for debugging
            if (logger.isDebugEnabled) {
                logger.debug("=== FULL AI RESPONSE ===")
                logger.debug(rawResponse)
                logger.debug("========================")
            }

            // Step 4: Parse and validate JSON response
            val parsedResponse = parseAndValidateResponse(rawResponse)

            // Log statistics
            logger.info("=== Generation Statistics ===")
            logger.info("Time: ${generationTimeMs}ms")
            logger.info("Characters: ${rawResponse.length}")
            logger.info("Lines: ${rawResponse.lines().size}")
            logger.info("Items: ${parsedResponse.categories.getTotalItemCount()}")
            logger.info("Total Quantity: ${parsedResponse.categories.getTotalQuantity()}")
            logger.info("============================")

            logger.info("✓ Packing list generated successfully")
            return parsedResponse

        } catch (e: AiServiceException) {
            logger.error("✗ AI service error: ${e.message}", e)

            if (useFallbackOnError) {
                logger.warn("⚠️ Falling back to dummy data due to error")
                return generateFallbackResponse(destination, durationDays, season, travelType)
            } else {
                throw e
            }

        } catch (e: Exception) {
            logger.error("✗ Unexpected error during packing list generation", e)

            if (useFallbackOnError) {
                logger.warn("⚠️ Falling back to dummy data due to unexpected error")
                return generateFallbackResponse(destination, durationDays, season, travelType)
            } else {
                throw AiServiceException("Unexpected error: ${e.message}", e)
            }
        }
    }

    /**
     * Calls the OpenAI API with error handling for common failure scenarios.
     *
     * @throws AuthenticationException if API key is invalid
     * @throws RateLimitException if rate limit is exceeded
     * @throws AiTimeoutException if request times out
     * @throws ServiceUnavailableException if API is unavailable
     */
    private fun callOpenAiApi(systemPrompt: String, userPrompt: String): String {
        try {
            logger.info("Calling OpenAI API...")

            val response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content()

            if (response.isNullOrBlank()) {
                throw ServiceUnavailableException("OpenAI returned null or empty response")
            }

            logger.info("✓ OpenAI API call successful")
            return response

        } catch (e: Exception) {
            // Map common Spring AI exceptions to our custom exceptions
            when {
                e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> {
                    throw AuthenticationException("Invalid API key or authentication failed", e)
                }
                e.message?.contains("429") == true || e.message?.contains("rate limit") == true -> {
                    throw RateLimitException("OpenAI rate limit exceeded", cause = e)
                }
                e.message?.contains("timeout") == true || e is TimeoutException -> {
                    throw AiTimeoutException("OpenAI request timed out", e)
                }
                e.message?.contains("503") == true || e.message?.contains("unavailable") == true -> {
                    throw ServiceUnavailableException("OpenAI service is temporarily unavailable", e)
                }
                else -> {
                    throw ServiceUnavailableException("OpenAI API call failed: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Parses and validates the raw JSON response from OpenAI.
     *
     * @throws InvalidJsonResponseException if JSON is malformed or doesn't match schema
     * @throws ValidationException if response fails validation rules
     */
    private fun parseAndValidateResponse(rawResponse: String): AiPackingResponse {
        try {
            logger.debug("Parsing JSON response...")

            // Extract JSON from response (sometimes AI includes markdown code blocks)
            val jsonContent = extractJsonFromResponse(rawResponse)

            // Parse JSON to data model
            val response: AiPackingResponse = objectMapper.readValue(jsonContent)

            logger.debug("✓ JSON parsed successfully")

            // Validate the response
            validateResponse(response)

            logger.debug("✓ Response validated successfully")

            return response

        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            logger.error("✗ Failed to parse JSON response", e)
            throw InvalidJsonResponseException(
                "Invalid JSON in AI response: ${e.message}",
                rawResponse = rawResponse,
                cause = e
            )
        } catch (e: IllegalArgumentException) {
            logger.error("✗ Response validation failed", e)
            throw ValidationException(
                "Response validation failed: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Extracts JSON content from the response, handling markdown code blocks.
     *
     * The AI might return JSON wrapped in markdown:
     * ```json
     * {...}
     * ```
     */
    private fun extractJsonFromResponse(response: String): String {
        val trimmed = response.trim()

        // Check if response is wrapped in markdown code block
        return if (trimmed.startsWith("```")) {
            // Remove markdown code fences
            val lines = trimmed.lines()
            val jsonLines = lines
                .dropWhile { it.startsWith("```") }
                .dropLastWhile { it.startsWith("```") || it.isBlank() }
            jsonLines.joinToString("\n")
        } else {
            trimmed
        }
    }

    /**
     * Validates the parsed response against business rules.
     *
     * @throws ValidationException if validation fails
     */
    private fun validateResponse(response: AiPackingResponse) {
        val errors = mutableListOf<String>()

        try {
            response.validate()
        } catch (e: IllegalArgumentException) {
            errors.add("Response validation failed: ${e.message}")
        }

        // Check for reasonable item counts (anti-hallucination check)
        val totalItems = response.categories.getTotalItemCount()
        if (totalItems < 3) {
            errors.add("Too few items ($totalItems) - expected at least 3")
        }
        if (totalItems > 100) {
            errors.add("Too many items ($totalItems) - seems unrealistic, possible hallucination")
        }

        // Check for reasonable quantities
        response.categories.getAllItems().forEach { item ->
            if (item.quantity < 0) {
                errors.add("Invalid quantity for ${item.item}: ${item.quantity}")
            }
            if (item.quantity > 50) {
                errors.add("Suspiciously high quantity for ${item.item}: ${item.quantity} - possible hallucination")
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(
                "Response validation failed with ${errors.size} error(s)",
                validationErrors = errors
            )
        }
    }

    /**
     * Generates a fallback response with dummy data when AI service fails.
     *
     * This ensures the application continues to function even when OpenAI is unavailable.
     */
    private fun generateFallbackResponse(
        destination: String,
        durationDays: Int,
        season: Season,
        travelType: TravelType
    ): AiPackingResponse {
        logger.info("Generating fallback response with dummy data for $destination ($season, $travelType)")

        // Create basic packing list based on travel type and season
        val clothing = mutableListOf<PackingItem>()
        val tech = mutableListOf<PackingItem>()
        val hygiene = mutableListOf<PackingItem>()
        val documents = mutableListOf<PackingItem>()
        val other = mutableListOf<PackingItem>()

        // Essential documents (always needed)
        documents.add(PackingItem("Passport", 1, "Required for international travel"))
        documents.add(PackingItem("Travel Insurance", 1, "Recommended for all trips"))

        // Basic clothing based on season
        when (season) {
            Season.SUMMER -> {
                clothing.add(PackingItem("T-Shirt", minOf(durationDays, 5), "Hot weather"))
                clothing.add(PackingItem("Shorts", minOf(durationDays / 2, 3), "Warm weather"))
                clothing.add(PackingItem("Swimwear", 1, "Summer activity"))
                other.add(PackingItem("Sunscreen", 1, "Sun protection"))
                other.add(PackingItem("Sunglasses", 1, "Eye protection"))
            }
            Season.WINTER -> {
                clothing.add(PackingItem("Warm Jacket", 1, "Cold weather"))
                clothing.add(PackingItem("Long Pants", minOf(durationDays, 3), "Cold weather"))
                clothing.add(PackingItem("Thermal Underwear", 2, "Extra warmth"))
                clothing.add(PackingItem("Gloves", 1, "Hand protection"))
                clothing.add(PackingItem("Warm Hat", 1, "Head protection"))
            }
            Season.SPRING, Season.FALL -> {
                clothing.add(PackingItem("Light Jacket", 1, "Variable weather"))
                clothing.add(PackingItem("Long Pants", minOf(durationDays / 2, 3), "Moderate weather"))
                clothing.add(PackingItem("T-Shirt", minOf(durationDays, 4), "Layering"))
                other.add(PackingItem("Umbrella", 1, "Rain protection"))
            }
        }

        // Tech essentials
        tech.add(PackingItem("Phone Charger", 1, "Essential device charging"))
        tech.add(PackingItem("Universal Adapter", 1, "International power compatibility"))

        // Hygiene basics
        hygiene.add(PackingItem("Toothbrush", 1, "Daily hygiene"))
        hygiene.add(PackingItem("Toothpaste", 1, "Daily hygiene"))
        hygiene.add(PackingItem("Deodorant", 1, "Personal hygiene"))

        // Business-specific items
        if (travelType == TravelType.BUSINESS) {
            clothing.add(PackingItem("Business Suit", 2, "Professional meetings"))
            clothing.add(PackingItem("Dress Shoes", 1, "Professional attire"))
            tech.add(PackingItem("Laptop", 1, "Work requirements"))
            tech.add(PackingItem("Laptop Charger", 1, "Device charging"))
        }

        // Backpacking-specific items
        if (travelType == TravelType.BACKPACKING) {
            other.add(PackingItem("Backpack", 1, "Main luggage"))
            other.add(PackingItem("Water Bottle", 1, "Hydration"))
            other.add(PackingItem("First Aid Kit", 1, "Safety"))
        }

        val categories = PackingCategories(
            clothing = clothing,
            tech = tech,
            hygiene = hygiene,
            documents = documents,
            other = other
        )

        logger.info("✓ Fallback response generated with ${categories.getTotalItemCount()} items")

        return AiPackingResponse(categories)
    }

    /**
     * Quick test method to verify OpenAI connectivity.
     * Returns a simple response for health check purposes.
     */
    fun testConnection(): String {
        logger.info("Testing OpenAI connection...")

        return try {
            val response = chatClient.prompt()
                .user("Respond with exactly: 'AI Service is working correctly'")
                .call()
                .content()

            logger.info("✓ Connection test successful: $response")
            response ?: "Connection successful but no response"
        } catch (e: Exception) {
            logger.error("✗ Connection test failed", e)
            throw IllegalStateException("OpenAI connection test failed: ${e.message}", e)
        }
    }
}
