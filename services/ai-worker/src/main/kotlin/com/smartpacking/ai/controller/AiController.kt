package com.smartpacking.ai.controller

import com.smartpacking.ai.mapper.PackingResponseMapper
import com.smartpacking.ai.service.AiService
import com.smartpacking.ai.service.CultureService
import com.smartpacking.ai.service.WeatherService
import com.smartpacking.shared.dto.PackingRequest
import com.smartpacking.shared.dto.PackingResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.system.measureTimeMillis

/**
 * REST controller for AI Worker packing list generation.
 *
 * Endpoints:
 * - POST /api/ai/generate - Generate a packing list based on travel parameters
 * - GET /api/ai/health - Health check endpoint
 */
@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiService: AiService,
    private val weatherService: WeatherService,
    private val cultureService: CultureService,
    private val mapper: PackingResponseMapper
) {
    private val logger = LoggerFactory.getLogger(AiController::class.java)

    /**
     * Generates a packing list using AI.
     *
     * @param request Validated packing request with destination, duration, season, travel type
     * @return Generated packing list with items categorized by type
     */
    @PostMapping("/generate")
    fun generatePackingList(
        @Valid @RequestBody request: PackingRequest
    ): ResponseEntity<PackingResponse> {
        logger.info("=== Received packing list generation request ===")
        logger.info("Destination: ${request.destination}, Duration: ${request.durationDays} days")
        logger.info("Season: ${request.season}, Travel Type: ${request.travelType}")

        // Generate packing list using AI
        var aiResponse: com.smartpacking.ai.model.AiPackingResponse
        var response: PackingResponse

        val generationTimeMs = measureTimeMillis {
            aiResponse = aiService.generatePackingList(
                destination = request.destination,
                durationDays = request.durationDays,
                season = request.season,
                travelType = request.travelType,
                useFallbackOnError = true // Use fallback for production resilience
            )

            // Gather contextual information
            val weatherInfo = weatherService.getWeatherInfo(request.destination, request.season)
            val cultureTips = cultureService.getCultureTips(request.destination)
            val cultureTipStrings = cultureTips.map { "${it.category}: ${it.tip}" }

            // Map to shared DTO
            response = mapper.toSharedDto(
                aiResponse = aiResponse,
                destination = request.destination,
                weatherInfo = weatherInfo,
                cultureTips = cultureTipStrings
            )
        }

        logger.info("✓ Packing list generated successfully in ${generationTimeMs}ms")
        logger.info("Total items: ${aiResponse.categories.getTotalItemCount()}")
        logger.info("Total quantity: ${aiResponse.categories.getTotalQuantity()}")

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Health check endpoint.
     *
     * @return Simple status message indicating the service is running
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        logger.debug("Health check requested")

        val response = mapOf(
            "status" to "UP",
            "service" to "ai-worker",
            "version" to "1.0.0",
            "aiProvider" to "OpenAI"
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Test AI connectivity endpoint.
     *
     * @return Connection status
     */
    @GetMapping("/test-connection")
    fun testConnection(): ResponseEntity<Map<String, String>> {
        logger.info("AI connection test requested")

        return try {
            val testResponse = aiService.testConnection()

            val response = mapOf(
                "status" to "connected",
                "message" to testResponse
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Connection test failed", e)

            val response = mapOf(
                "status" to "failed",
                "message" to (e.message ?: "Connection test failed")
            )

            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
        }
    }
}
