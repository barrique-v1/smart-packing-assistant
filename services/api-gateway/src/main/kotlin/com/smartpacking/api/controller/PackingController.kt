package com.smartpacking.api.controller

import com.smartpacking.api.exception.ValidationException
import com.smartpacking.api.service.PackingListService
import com.smartpacking.shared.dto.PackingRequest
import com.smartpacking.shared.dto.PackingResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST controller for packing list operations
 */
@RestController
@RequestMapping("/api/packing")
@CrossOrigin(origins = ["http://localhost:5173"]) // For React frontend
class PackingController(
    private val packingListService: PackingListService
) {

    private val logger = LoggerFactory.getLogger(PackingController::class.java)

    /**
     * Generate a new packing list
     * POST /api/packing/generate
     *
     * Requires a session token in the X-Session-Token header
     */
    @PostMapping("/generate")
    fun generatePackingList(
        @Valid @RequestBody request: PackingRequest,
        @RequestHeader("X-Session-Token") sessionToken: String
    ): ResponseEntity<PackingResponse> {
        logger.info("Generating packing list for ${request.destination}")

        // Validate request
        validatePackingRequest(request)

        // Generate packing list
        val response = packingListService.generatePackingList(request, sessionToken)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Get a specific packing list by ID
     * GET /api/packing/{id}
     */
    @GetMapping("/{id}")
    fun getPackingList(@PathVariable id: UUID): ResponseEntity<PackingResponse> {
        logger.info("Getting packing list: $id")

        val response = packingListService.getPackingList(id)

        return ResponseEntity.ok(response)
    }

    /**
     * Get all packing lists for the current session
     * GET /api/packing/session
     *
     * Requires a session token in the X-Session-Token header
     */
    @GetMapping("/session")
    fun getPackingListsBySession(
        @RequestHeader("X-Session-Token") sessionToken: String
    ): ResponseEntity<List<PackingResponse>> {
        logger.info("Getting packing lists for session")

        val responses = packingListService.getPackingListsBySession(sessionToken)

        return ResponseEntity.ok(responses)
    }

    /**
     * Get recent packing lists for the current session
     * GET /api/packing/session/recent
     *
     * Requires a session token in the X-Session-Token header
     */
    @GetMapping("/session/recent")
    fun getRecentPackingLists(
        @RequestHeader("X-Session-Token") sessionToken: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<PackingResponse>> {
        logger.info("Getting recent packing lists for session (limit: $limit)")

        val responses = packingListService.getRecentPackingLists(sessionToken, limit)

        return ResponseEntity.ok(responses)
    }

    /**
     * Search packing lists by destination
     * GET /api/packing/search?destination={query}
     */
    @GetMapping("/search")
    fun searchPackingLists(
        @RequestParam destination: String
    ): ResponseEntity<List<PackingResponse>> {
        logger.info("Searching packing lists for destination: $destination")

        if (destination.isBlank()) {
            throw ValidationException("Destination query cannot be empty")
        }

        val responses = packingListService.searchByDestination(destination)

        return ResponseEntity.ok(responses)
    }

    /**
     * Health check endpoint for packing service
     * GET /api/packing/health
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "packing-api"
            )
        )
    }

    /**
     * Validate packing request
     */
    private fun validatePackingRequest(request: PackingRequest) {
        if (request.destination.isBlank()) {
            throw ValidationException("Destination is required")
        }

        if (request.durationDays <= 0) {
            throw ValidationException("Duration must be positive")
        }

        if (request.durationDays > 365) {
            throw ValidationException("Duration cannot exceed 365 days")
        }

        // TravelType and Season are already enums, so validation is built-in
    }
}
