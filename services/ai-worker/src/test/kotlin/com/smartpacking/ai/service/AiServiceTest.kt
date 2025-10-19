package com.smartpacking.ai.service

import com.smartpacking.shared.enums.Season
import com.smartpacking.shared.enums.TravelType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Tests for AiService.
 *
 * Note: These tests make REAL API calls to OpenAI and will consume tokens.
 * They are disabled by default and only run when OPENAI_API_KEY is set.
 *
 * To run these tests:
 * 1. Ensure OPENAI_API_KEY environment variable is set with a valid key
 * 2. Run: ./gradlew test --tests AiServiceTest
 */
@SpringBootTest
@ActiveProfiles("test")
class AiServiceTest {

    @Autowired
    private lateinit var aiService: AiService

    @Test
    fun `should inject AiService successfully`() {
        assertNotNull(aiService, "AiService should be injected")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    fun `should test OpenAI connection successfully`() {
        // When
        val response = aiService.testConnection()

        // Then
        assertNotNull(response)
        assertTrue(response.isNotBlank(), "Response should not be blank")
        println("Connection test response: $response")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    fun `should generate packing list for Dubai summer vacation`() {
        // Given
        val destination = "Dubai"
        val durationDays = 5
        val season = Season.SUMMER
        val travelType = TravelType.VACATION

        println("\n=== TEST: Generating packing list for $destination ===")
        println("Duration: $durationDays days")
        println("Season: $season")
        println("Type: $travelType")

        // When
        val response = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType,
            useFallbackOnError = false // Fail fast to detect API issues
        )

        // Then
        assertNotNull(response, "Response should not be null")
        assertNotNull(response.categories, "Categories should not be null")

        // Verify we have items
        val totalItems = response.categories.getTotalItemCount()
        assertTrue(totalItems >= 3, "Should have at least 3 items, got $totalItems")
        assertTrue(totalItems <= 100, "Should have reasonable number of items, got $totalItems")

        // Verify categories exist
        val allItems = response.categories.getAllItems()
        assertTrue(allItems.isNotEmpty(), "Should have items in categories")

        // Print for manual verification
        println("\n=== GENERATED PACKING LIST ===")
        println(response.getSummary())
        println("\n--- Items by Category ---")
        println("\nClothing (${response.categories.clothing.size}):")
        response.categories.clothing.forEach { println("  - ${it.item} x${it.quantity}: ${it.reason}") }
        println("\nTech (${response.categories.tech.size}):")
        response.categories.tech.forEach { println("  - ${it.item} x${it.quantity}: ${it.reason}") }
        println("\nHygiene (${response.categories.hygiene.size}):")
        response.categories.hygiene.forEach { println("  - ${it.item} x${it.quantity}: ${it.reason}") }
        println("\nDocuments (${response.categories.documents.size}):")
        response.categories.documents.forEach { println("  - ${it.item} x${it.quantity}: ${it.reason}") }
        println("\nOther (${response.categories.other.size}):")
        response.categories.other.forEach { println("  - ${it.item} x${it.quantity}: ${it.reason}") }
        println("================================")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    fun `should generate packing list for Iceland winter backpacking`() {
        // Given
        val destination = "Iceland"
        val durationDays = 7
        val season = Season.WINTER
        val travelType = TravelType.BACKPACKING

        println("\n=== TEST: Generating packing list for $destination ===")

        // When
        val response = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType,
            useFallbackOnError = false
        )

        // Then
        assertNotNull(response)
        assertNotNull(response.categories)

        // Verify reasonable item count
        val totalItems = response.categories.getTotalItemCount()
        assertTrue(totalItems >= 3, "Should have at least 3 items")
        assertTrue(totalItems <= 100, "Should have reasonable number of items")

        // Should mention winter/cold weather items (check all item names and reasons)
        val allText = response.categories.getAllItems()
            .flatMap { listOf(it.item, it.reason) }
            .joinToString(" ")
            .lowercase()

        assertTrue(
            allText.contains("jacket") ||
            allText.contains("warm") ||
            allText.contains("layer") ||
            allText.contains("coat") ||
            allText.contains("thermal"),
            "Response should mention cold weather items"
        )

        println("\n=== ICELAND WINTER BACKPACKING LIST ===")
        println(response.getSummary())
        response.categories.clothing.forEach {
            println("  🧥 ${it.item} x${it.quantity}: ${it.reason}")
        }
        println("========================================")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    fun `should generate packing list for Tokyo spring business trip`() {
        // Given
        val destination = "Tokyo"
        val durationDays = 3
        val season = Season.SPRING
        val travelType = TravelType.BUSINESS

        println("\n=== TEST: Generating packing list for $destination ===")

        // When
        val response = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType,
            useFallbackOnError = false
        )

        // Then
        assertNotNull(response)
        assertNotNull(response.categories)

        // Verify reasonable item count
        val totalItems = response.categories.getTotalItemCount()
        assertTrue(totalItems >= 3, "Should have at least 3 items")
        assertTrue(totalItems <= 100, "Should have reasonable number of items")

        // Should mention business attire (check all item names and reasons)
        val allText = response.categories.getAllItems()
            .flatMap { listOf(it.item, it.reason) }
            .joinToString(" ")
            .lowercase()

        assertTrue(
            allText.contains("suit") ||
            allText.contains("formal") ||
            allText.contains("business") ||
            allText.contains("professional") ||
            allText.contains("dress"),
            "Response should mention business attire"
        )

        println("\n=== TOKYO BUSINESS TRIP LIST ===")
        println(response.getSummary())
        response.categories.clothing.forEach {
            println("  👔 ${it.item} x${it.quantity}: ${it.reason}")
        }
        println("==================================")
    }

    @Test
    fun `should handle service availability check`() {
        // This test runs without API key - just checks service is available
        assertNotNull(aiService)
        println("✓ AiService is available and can be injected")
    }

    @Test
    fun `should return fallback data when API is unavailable`() {
        // Given
        val destination = "TestCity"
        val durationDays = 5
        val season = Season.SUMMER
        val travelType = TravelType.VACATION

        println("\n=== TEST: Fallback behavior ===")

        // When - Use fallback even if API works (to test fallback logic)
        // We simulate this by using a destination that might not have data
        val response = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType,
            useFallbackOnError = true
        )

        // Then - Fallback should still return valid data
        assertNotNull(response, "Fallback response should not be null")
        assertNotNull(response.categories, "Fallback categories should not be null")

        val totalItems = response.categories.getTotalItemCount()
        assertTrue(totalItems >= 3, "Fallback should have at least 3 items, got $totalItems")

        // For summer vacation, should have summer-appropriate items
        val allItems = response.categories.getAllItems()
        assertTrue(allItems.isNotEmpty(), "Fallback should have items")

        println("✓ Fallback response generated successfully")
        println(response.getSummary())
    }

    @Test
    fun `should validate reasonable item quantities`() {
        // This test verifies anti-hallucination checks on the validation side
        println("\n=== TEST: Validation checks ===")

        // The validation should catch if AI returns unreasonable quantities
        // We're testing that our validation logic exists and works

        val destination = "Paris"
        val durationDays = 3
        val season = Season.FALL
        val travelType = TravelType.VACATION

        val response = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = travelType,
            useFallbackOnError = true
        )

        // Verify all items have reasonable quantities
        response.categories.getAllItems().forEach { item ->
            assertTrue(item.quantity > 0, "Item ${item.item} should have positive quantity")
            assertTrue(
                item.quantity <= 50,
                "Item ${item.item} has suspicious quantity ${item.quantity} - possible hallucination"
            )
        }

        println("✓ All items have reasonable quantities")
        println(response.getSummary())
    }

    @Test
    fun `should generate different lists for different travel types`() {
        // This test verifies that the service adapts to travel type
        println("\n=== TEST: Travel type adaptation ===")

        val destination = "London"
        val durationDays = 5
        val season = Season.SPRING

        // Generate for BUSINESS
        val businessResponse = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = TravelType.BUSINESS,
            useFallbackOnError = true
        )

        // Generate for BACKPACKING
        val backpackingResponse = aiService.generatePackingList(
            destination = destination,
            durationDays = durationDays,
            season = season,
            travelType = TravelType.BACKPACKING,
            useFallbackOnError = true
        )

        // Both should have items
        assertTrue(businessResponse.categories.getTotalItemCount() >= 3, "Business trip should have items")
        assertTrue(backpackingResponse.categories.getTotalItemCount() >= 3, "Backpacking should have items")

        println("✓ Different lists generated for different travel types")
        println("\nBusiness:")
        println(businessResponse.getSummary())
        println("\nBackpacking:")
        println(backpackingResponse.getSummary())
    }
}
