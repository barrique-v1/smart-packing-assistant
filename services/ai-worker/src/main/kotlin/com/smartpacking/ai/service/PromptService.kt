package com.smartpacking.ai.service

import com.smartpacking.ai.model.CultureTip
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
 * - Anti-hallucination safeguards
 */
@Service
class PromptService(
    private val weatherService: WeatherService,
    private val cultureService: CultureService
) {
    private val logger = LoggerFactory.getLogger(PromptService::class.java)

    /**
     * System prompt with anti-hallucination guidelines and output format specification.
     *
     * Key principles:
     * - Reliability over creativity (temperature 0.3)
     * - Only suggest practical, commonly available items
     * - Use provided weather and cultural context
     * - Structured JSON output format
     * - Admit uncertainty rather than hallucinate
     */
    private val systemPrompt = """
        You are a practical travel packing assistant focused on providing reliable, realistic advice.

        CRITICAL RULES - Anti-Hallucination Guidelines:
        1. Only suggest items that are commonly available and practical for travel
        2. Base recommendations strictly on the provided weather and cultural information
        3. If you don't have specific information about a destination, say "I'm not sure" rather than guessing
        4. Do not invent fictional items, brands, or requirements
        5. Focus on essential items - avoid over-packing suggestions
        6. All quantities must be reasonable for the trip duration

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

        ITEM SELECTION GUIDELINES:
        - Clothing: Based on weather (temperature, rain, humidity), duration, and cultural norms
        - Tech: Universal items (chargers, adapters, power banks) - no exotic gadgets
        - Hygiene: Basic toiletries, medications if needed for climate
        - Documents: Passport, visa if needed, travel insurance
        - Other: Weather-specific items (umbrella, sunscreen), activity-specific items

        Remember: Practical and reliable beats creative and excessive. When in doubt, keep it simple.
        
        
        CRITICAL VALIDATION CHECKPOINT
        Your response MUST include ALL of these items (no exceptions):
        1. Underwear  2. Socks  3. Shirts  4. Pants  5. Footwear  6. Sleepwear
        7. Phone Charger  8. Power Adapter  9. Toothbrush  10. Toothpaste
        11. Deodorant  12. Shampoo  13. Passport  14. Travel Insurance
        YOUR OUTPUT IS INCORRECT IF ANY OF THESE ITEMS ARE MISSING - PLEASE ENSURE THEY ARE INCLUDED.

    """.trimIndent()

    /**
     * Builds a complete prompt for packing list generation.
     *
     * Combines system prompt, user request, and contextual data (weather, culture).
     *
     * @param destination Travel destination
     * @param durationDays Trip duration in days
     * @param season Season of travel
     * @param travelType Type of travel (BUSINESS, VACATION, BACKPACKING)
     * @return Complete prompt string ready for AI processing
     */
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

        // Build user prompt with all context
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
     * Builds the user-specific prompt with all contextual information.
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
     */
    fun getSystemPrompt(): String = systemPrompt
}
