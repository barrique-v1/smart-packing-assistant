package com.smartpacking.ai.service

import com.smartpacking.ai.model.CultureTip
import com.smartpacking.shared.dto.PackingRequest
import com.smartpacking.shared.enums.Season
import com.smartpacking.shared.enums.TravelType
import com.smartpacking.shared.model.WeatherInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for building AI prompts with anti-hallucination guidelines.
 *
 * This service constructs prompts for packing list generation with:
 * - Clear instructions for reliability over creativity
 * - Structured JSON output format
 * - Context from weather and cultural data
 * - RAG-retrieved expert items from vector database
 * - Anti-hallucination safeguards
 */
@Service
class PromptService(
    private val weatherService: WeatherService,
    private val cultureService: CultureService
) {
    private val logger = LoggerFactory.getLogger(PromptService::class.java)

    /**
     * Builds system prompt dynamically based on RAG retrieval status.
     *
     * If retrievedItems is not empty:
     * - Instructs GPT to PRIORITIZE expert-verified items from knowledge base
     * - Only add items that complement (not duplicate) retrieved items
     *
     * If retrievedItems is empty:
     * - Falls back to pure GPT generation with conservative guidelines
     */
    private fun buildSystemPrompt(retrievedItems: List<RetrievedItem>): String {
        return """
        You are an expert packing assistant. Generate a categorized packing list.

        IMPORTANT INSTRUCTIONS:
        ${if (retrievedItems.isNotEmpty()) {
            """
            1. You have access to ${retrievedItems.size} expert-verified items from the knowledge base.
            2. PRIORITIZE these items - they are proven recommendations with confidence scores.
            3. Use the retrieved items as your PRIMARY source.
            4. Only add additional items if:
               - They are absolutely essential for this specific trip
               - They complement the retrieved items
               - They address unique aspects not covered by the knowledge base
            5. DO NOT duplicate retrieved items.
            6. Maintain the quantity and reason from retrieved items unless context requires adjustment.
            """
        } else {
            """
            1. The knowledge base search returned no items (this is unusual).
            2. Generate recommendations based on your training, but be conservative.
            3. Focus on universal essentials and context-specific needs.
            """
        }}

        OUTPUT FORMAT:
        You must respond with ONLY valid JSON in this exact structure (no markdown, no explanations):
        {
          "categories": {
            "clothing": [
              {"item": "T-Shirt", "quantity": 3, "reason": "Hot weather, change daily"}
            ],
            "tech": [
              {"item": "Phone Charger", "quantity": 1, "reason": "Essential for devices"}
            ],
            "hygiene": [
              {"item": "Toothbrush", "quantity": 1, "reason": "Daily hygiene"}
            ],
            "documents": [
              {"item": "Passport", "quantity": 1, "reason": "Required for international travel"}
            ],
            "other": [
              {"item": "Sunglasses", "quantity": 1, "reason": "Sun protection"}
            ]
          }
        }

        CRITICAL RULES - Anti-Hallucination Guidelines:
        - DO NOT invent fictional items
        - DO NOT suggest dangerous or illegal items
        - DO NOT over-pack (be practical)
        - If unsure, use retrieved items or omit
        - Total items: 3-100
        - Quantity per item: 1-50

        CRITICAL VALIDATION CHECKPOINT:
        Your response MUST include ALL of these items (no exceptions):
        1. Underwear  2. Socks  3. Shirts  4. Pants  5. Footwear  6. Sleepwear
        7. Phone Charger  8. Power Adapter  9. Toothbrush  10. Toothpaste
        11. Deodorant  12. Shampoo  13. Passport  14. Travel Insurance
        YOUR OUTPUT IS INCORRECT IF ANY OF THESE ITEMS ARE MISSING - PLEASE ENSURE THEY ARE INCLUDED.
    """.trimIndent()
    }

    /**
     * Builds a complete prompt for packing list generation with RAG support.
     *
     * NEW: This is the main entry point for RAG-enhanced prompt generation.
     *
     * @param request Packing request with trip details
     * @param weatherInfo Weather data (can be null)
     * @param cultureTips List of cultural considerations
     * @param retrievedItems Expert items from vector database (empty list if none found)
     * @return Complete prompt string ready for AI processing
     */
    fun buildPrompt(
        request: PackingRequest,
        weatherInfo: WeatherInfo?,
        cultureTips: List<CultureTip>,
        retrievedItems: List<RetrievedItem> = emptyList()
    ): String {
        logger.debug("Building RAG-enhanced prompt for: ${request.destination}, ${request.durationDays} days, ${request.season}, ${request.travelType}")
        logger.debug("Retrieved items: ${retrievedItems.size}, Weather: ${weatherInfo != null}, Culture tips: ${cultureTips.size}")

        val systemPrompt = buildSystemPrompt(retrievedItems)
        val userPrompt = buildUserPromptWithRAG(request, weatherInfo, cultureTips, retrievedItems)

        return "$systemPrompt\n\n$userPrompt"
    }

    /**
     * DEPRECATED: Use buildPrompt(request, weatherInfo, cultureTips, retrievedItems) instead.
     *
     * Legacy method for backward compatibility. This will fetch weather/culture internally
     * but does not support RAG retrieval.
     *
     * @param destination Travel destination
     * @param durationDays Trip duration in days
     * @param season Season of travel
     * @param travelType Type of travel (BUSINESS, VACATION, BACKPACKING)
     * @return Complete prompt string ready for AI processing
     */
    @Deprecated("Use buildPrompt with PackingRequest and retrievedItems for RAG support")
    fun buildPackingListPrompt(
        destination: String,
        durationDays: Int,
        season: Season,
        travelType: TravelType
    ): String {
        logger.debug("Building prompt for: $destination, $durationDays days, $season, $travelType")

        // Gather contextual information
        val weatherInfo = weatherService.getWeatherInfo(destination, season)
        val cultureTips = cultureService.getCultureTips(destination)

        // Build user prompt with all context (no RAG)
        val userPrompt = buildUserPrompt(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType,
            weatherInfo = weatherInfo,
            cultureTips = cultureTips
        )

        logger.debug("Prompt built successfully with weather and ${cultureTips.size} culture tips")

        return userPrompt
    }

    /**
     * NEW: Builds the user-specific prompt with RAG-retrieved items.
     *
     * Includes:
     * - Trip details (destination, duration, season, travel type)
     * - Weather information
     * - Cultural considerations
     * - Retrieved expert items formatted by category with confidence scores
     */
    private fun buildUserPromptWithRAG(
        request: PackingRequest,
        weatherInfo: WeatherInfo?,
        cultureTips: List<CultureTip>,
        retrievedItems: List<RetrievedItem>
    ): String {
        return buildString {
            appendLine("Generate a packing list for the following trip:")
            appendLine()

            // Trip details
            appendLine("TRIP DETAILS:")
            appendLine("- Destination: ${request.destination}")
            appendLine("- Duration: ${request.durationDays} days")
            appendLine("- Season: ${request.season}")
            appendLine("- Travel Type: ${formatTravelType(request.travelType)}")
            appendLine()

            // Weather information
            if (weatherInfo != null) {
                appendLine("WEATHER CONDITIONS:")
                appendLine("- Temperature: ${weatherInfo.tempMin}°C to ${weatherInfo.tempMax}°C")
                appendLine("- Conditions: ${weatherInfo.conditions}")
                appendLine("- Humidity: ${weatherInfo.humidity}")
                appendLine("- Rain Chance: ${weatherInfo.precipitationChance}%")
                appendLine()
            }

            // Cultural considerations
            if (cultureTips.isNotEmpty()) {
                appendLine("CULTURAL CONSIDERATIONS:")
                cultureTips.forEach { tip ->
                    val importanceMarker = when (tip.importance.uppercase()) {
                        "HIGH" -> "⚠️ IMPORTANT"
                        "MEDIUM" -> "ℹ️"
                        else -> "•"
                    }
                    appendLine("$importanceMarker [${tip.category}] ${tip.tip}")
                }
                appendLine()
            }

            // RAG-retrieved expert items (NEW)
            if (retrievedItems.isNotEmpty()) {
                appendLine("RETRIEVED EXPERT ITEMS (${retrievedItems.size} items):")
                appendLine("Use these as your foundation:")
                appendLine()

                // Group by category for better readability
                retrievedItems.groupBy { it.category }.forEach { (category, items) ->
                    appendLine("${category.uppercase()}:")
                    items.sortedByDescending { it.score }.forEach { item ->
                        appendLine("  - ${item.item} x${item.quantity}")
                        appendLine("    Reason: ${item.reason}")
                        appendLine("    Confidence: ${String.format("%.1f%%", item.score * 100)}")
                        if (item.tags.isNotEmpty()) {
                            appendLine("    Tags: ${item.tags.joinToString(", ")}")
                        }
                    }
                    appendLine()
                }
            }

            // Travel type specific instructions
            appendLine("TRAVEL TYPE CONSIDERATIONS:")
            when (request.travelType) {
                TravelType.BUSINESS -> {
                    appendLine("- Include professional attire (suits, dress shirts, formal shoes)")
                    appendLine("- Include laptop, chargers, and business documents")
                    appendLine("- Focus on wrinkle-resistant, professional clothing")
                }
                TravelType.VACATION -> {
                    appendLine("- Include comfortable casual wear")
                    appendLine("- Include leisure items (swimwear if warm, camera, etc.)")
                    appendLine("- Balance comfort and style")
                }
                TravelType.BACKPACKING -> {
                    appendLine("- Prioritize lightweight, multi-purpose items")
                    appendLine("- Include durable, quick-dry clothing")
                    appendLine("- Minimize quantity, maximize versatility")
                }
            }
            appendLine()

            appendLine("Generate a complete, categorized packing list in JSON format.")
        }
    }

    /**
     * LEGACY: Builds the user-specific prompt with all contextual information.
     * Used by deprecated buildPackingListPrompt method.
     */
    private fun buildUserPrompt(
        destination: String,
        durationDays: Int,
        season: Season,
        travelType: TravelType,
        weatherInfo: WeatherInfo?,
        cultureTips: List<CultureTip>
    ): String {
        val prompt = StringBuilder()

        // Trip details
        prompt.appendLine("Generate a packing list for the following trip:")
        prompt.appendLine()
        prompt.appendLine("TRIP DETAILS:")
        prompt.appendLine("- Destination: $destination")
        prompt.appendLine("- Duration: $durationDays days")
        prompt.appendLine("- Season: ${season.name}")
        prompt.appendLine("- Travel Type: ${formatTravelType(travelType)}")
        prompt.appendLine()

        // Weather information
        if (weatherInfo != null) {
            prompt.appendLine("WEATHER CONDITIONS:")
            prompt.appendLine("- Temperature: ${weatherInfo.tempMin}°C to ${weatherInfo.tempMax}°C")
            prompt.appendLine("- Conditions: ${weatherInfo.conditions}")
            prompt.appendLine("- Humidity: ${weatherInfo.humidity}")
            prompt.appendLine("- Rain Chance: ${weatherInfo.precipitationChance}%")
            prompt.appendLine()
        } else {
            prompt.appendLine("WEATHER CONDITIONS:")
            prompt.appendLine("- No specific weather data available for this destination")
            prompt.appendLine("- Use general seasonal recommendations for ${season.name}")
            prompt.appendLine()
        }

        // Cultural considerations
        if (cultureTips.isNotEmpty()) {
            prompt.appendLine("CULTURAL CONSIDERATIONS:")
            cultureTips.forEach { tip ->
                val importanceMarker = when (tip.importance.uppercase()) {
                    "HIGH" -> "⚠️ IMPORTANT"
                    "MEDIUM" -> "ℹ️"
                    else -> "•"
                }
                prompt.appendLine("$importanceMarker [${tip.category}] ${tip.tip}")
            }
            prompt.appendLine()
        }

        // Travel type specific instructions
        prompt.appendLine("TRAVEL TYPE CONSIDERATIONS:")
        when (travelType) {
            TravelType.BUSINESS -> {
                prompt.appendLine("- Include professional attire (suits, dress shirts, formal shoes)")
                prompt.appendLine("- Include laptop, chargers, and business documents")
                prompt.appendLine("- Focus on wrinkle-resistant, professional clothing")
            }
            TravelType.VACATION -> {
                prompt.appendLine("- Include comfortable casual wear")
                prompt.appendLine("- Include leisure items (swimwear if warm, camera, etc.)")
                prompt.appendLine("- Balance comfort and style")
            }
            TravelType.BACKPACKING -> {
                prompt.appendLine("- Prioritize lightweight, multi-purpose items")
                prompt.appendLine("- Include durable, quick-dry clothing")
                prompt.appendLine("- Minimize quantity, maximize versatility")
            }
        }
        prompt.appendLine()

        // Final instructions
        prompt.appendLine("Generate a practical packing list following the JSON format specified in the system prompt.")
        prompt.appendLine("Base all recommendations on the provided weather and cultural information.")
        prompt.appendLine("Keep quantities realistic for a $durationDays-day trip.")

        return prompt.toString()
    }

    /**
     * Formats travel type for display.
     */
    private fun formatTravelType(travelType: TravelType): String {
        return when (travelType) {
            TravelType.BUSINESS -> "Business Trip"
            TravelType.VACATION -> "Vacation"
            TravelType.BACKPACKING -> "Backpacking Adventure"
        }
    }

    /**
     * Returns the system prompt for testing/debugging purposes.
     * Returns the version with empty retrievedItems (legacy mode).
     */
    fun getSystemPrompt(): String = buildSystemPrompt(emptyList())
}
