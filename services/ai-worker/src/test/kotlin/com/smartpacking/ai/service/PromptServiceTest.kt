package com.smartpacking.ai.service

import com.smartpacking.shared.enums.Season
import com.smartpacking.shared.enums.TravelType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Tests for PromptService to verify prompt generation.
 */
@SpringBootTest
@ActiveProfiles("test")
class PromptServiceTest {

    @Autowired
    private lateinit var promptService: PromptService

    @Test
    fun `should generate prompt for business trip to Dubai in summer`() {
        // Given
        val destination = "Dubai"
        val durationDays = 5
        val season = Season.SUMMER
        val travelType = TravelType.BUSINESS

        // When
        val prompt = promptService.buildPackingListPrompt(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType
        )

        // Then
        assertNotNull(prompt)
        assertTrue(prompt.isNotBlank(), "Prompt should not be blank")

        // Verify trip details are included
        assertTrue(prompt.contains(destination), "Should include destination")
        assertTrue(prompt.contains(durationDays.toString()), "Should include duration")
        assertTrue(prompt.contains(season.name), "Should include season")

        // Verify weather information is included (Dubai has weather data)
        assertTrue(prompt.contains("WEATHER CONDITIONS"), "Should include weather section")
        assertTrue(prompt.contains("Temperature"), "Should include temperature")

        // Verify cultural considerations
        assertTrue(prompt.contains("CULTURAL CONSIDERATIONS"), "Should include cultural section")

        // Verify travel type specific instructions
        assertTrue(prompt.contains("TRAVEL TYPE CONSIDERATIONS"), "Should include travel type section")
        assertTrue(prompt.contains("professional attire"), "Business trip should mention professional attire")

        println("Generated prompt length: ${prompt.length} characters")
        println("\n=== SAMPLE PROMPT ===\n$prompt\n==================")
    }

    @Test
    fun `should generate prompt for vacation to Iceland in winter`() {
        // Given
        val destination = "Iceland"
        val durationDays = 7
        val season = Season.WINTER
        val travelType = TravelType.VACATION

        // When
        val prompt = promptService.buildPackingListPrompt(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType
        )

        // Then
        assertNotNull(prompt)
        assertTrue(prompt.contains("Iceland"), "Should include Iceland")
        assertTrue(prompt.contains("WINTER"), "Should include winter season")
        assertTrue(prompt.contains("casual wear"), "Vacation should mention casual wear")

        // Iceland should have weather data
        assertTrue(prompt.contains("Temperature"), "Should include temperature info")

        println("\n=== ICELAND WINTER VACATION PROMPT ===\n$prompt\n==================")
    }

    @Test
    fun `should generate prompt for backpacking to Tokyo in spring`() {
        // Given
        val destination = "Tokyo"
        val durationDays = 10
        val season = Season.SPRING
        val travelType = TravelType.BACKPACKING

        // When
        val prompt = promptService.buildPackingListPrompt(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType
        )

        // Then
        assertNotNull(prompt)
        assertTrue(prompt.contains("Tokyo"), "Should include Tokyo")
        assertTrue(prompt.contains("SPRING"), "Should include spring season")
        assertTrue(prompt.contains("lightweight"), "Backpacking should mention lightweight items")
        assertTrue(prompt.contains("multi-purpose"), "Backpacking should mention multi-purpose items")

        println("\n=== TOKYO SPRING BACKPACKING PROMPT ===\n$prompt\n==================")
    }

    @Test
    fun `should handle unknown destination gracefully`() {
        // Given
        val destination = "UnknownCity"
        val durationDays = 3
        val season = Season.SUMMER
        val travelType = TravelType.VACATION

        // When
        val prompt = promptService.buildPackingListPrompt(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType
        )

        // Then
        assertNotNull(prompt)
        assertTrue(prompt.contains("UnknownCity"), "Should include destination name")
        // Should indicate no specific weather data
        assertTrue(
            prompt.contains("No specific weather data") || prompt.contains("general seasonal"),
            "Should indicate missing weather data"
        )

        println("\n=== UNKNOWN DESTINATION PROMPT ===\n$prompt\n==================")
    }

    @Test
    fun `system prompt should contain anti-hallucination guidelines`() {
        // When
        val systemPrompt = promptService.getSystemPrompt()

        // Then
        assertNotNull(systemPrompt)
        assertTrue(systemPrompt.contains("Anti-Hallucination"), "Should include anti-hallucination section")
        assertTrue(systemPrompt.contains("reliable"), "Should emphasize reliability")
        assertTrue(systemPrompt.contains("JSON"), "Should specify JSON format")
        assertTrue(systemPrompt.contains("categories"), "Should define categories structure")

        // Verify all required categories are mentioned
        assertTrue(systemPrompt.contains("clothing"), "Should mention clothing category")
        assertTrue(systemPrompt.contains("tech"), "Should mention tech category")
        assertTrue(systemPrompt.contains("hygiene"), "Should mention hygiene category")
        assertTrue(systemPrompt.contains("documents"), "Should mention documents category")
        assertTrue(systemPrompt.contains("other"), "Should mention other category")

        println("\n=== SYSTEM PROMPT LENGTH ===")
        println("Characters: ${systemPrompt.length}")
        println("Lines: ${systemPrompt.lines().size}")
    }

    @Test
    fun `should include all parameters in prompt for each travel type`() {
        val destination = "Dubai"
        val durationDays = 5
        val season = Season.SUMMER

        // Test each travel type
        listOf(TravelType.BUSINESS, TravelType.VACATION, TravelType.BACKPACKING).forEach { travelType ->
            val prompt = promptService.buildPackingListPrompt(
                destination = destination,
                durationDays = durationDays,
                season = season,
                travelType = travelType
            )

            // All prompts should have these sections
            assertTrue(prompt.contains("TRIP DETAILS"), "Should have trip details for $travelType")
            assertTrue(prompt.contains("WEATHER CONDITIONS"), "Should have weather for $travelType")
            assertTrue(prompt.contains("TRAVEL TYPE CONSIDERATIONS"), "Should have travel type info for $travelType")

            println("✓ $travelType prompt validated")
        }
    }
}
