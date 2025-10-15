package com.smartpacking.api.controller

import com.smartpacking.api.service.SessionService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST controller for session management
 */
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = ["http://localhost:5173"]) // For React frontend
class SessionController(
    private val sessionService: SessionService
) {

    private val logger = LoggerFactory.getLogger(SessionController::class.java)

    /**
     * Create a new session
     * POST /api/sessions
     */
    @PostMapping
    fun createSession(): ResponseEntity<SessionResponse> {
        logger.info("Creating new session")

        val session = sessionService.createSession()

        val response = SessionResponse(
            sessionId = session.id!!,
            sessionToken = session.sessionToken,
            createdAt = session.createdAt.toString(),
            isActive = session.isActive
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Get session info by token
     * GET /api/sessions/{token}
     */
    @GetMapping("/{token}")
    fun getSession(@PathVariable token: String): ResponseEntity<SessionResponse> {
        logger.info("Getting session: $token")

        val session = sessionService.findByToken(token)

        val response = SessionResponse(
            sessionId = session.id!!,
            sessionToken = session.sessionToken,
            createdAt = session.createdAt.toString(),
            isActive = session.isActive
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Validate a session token
     * GET /api/sessions/{token}/validate
     */
    @GetMapping("/{token}/validate")
    fun validateSession(@PathVariable token: String): ResponseEntity<ValidationResponse> {
        logger.info("Validating session: $token")

        val isValid = sessionService.validateSession(token)

        return ResponseEntity.ok(ValidationResponse(isValid = isValid))
    }

    /**
     * Get all active sessions (for admin/debugging)
     * GET /api/sessions
     */
    @GetMapping
    fun getActiveSessions(): ResponseEntity<List<SessionResponse>> {
        logger.info("Getting all active sessions")

        val sessions = sessionService.getActiveSessions()

        val responses = sessions.map { session ->
            SessionResponse(
                sessionId = session.id!!,
                sessionToken = session.sessionToken,
                createdAt = session.createdAt.toString(),
                isActive = session.isActive
            )
        }

        return ResponseEntity.ok(responses)
    }

    /**
     * Cleanup inactive sessions
     * POST /api/sessions/cleanup
     */
    @PostMapping("/cleanup")
    fun cleanupInactiveSessions(): ResponseEntity<CleanupResponse> {
        logger.info("Cleaning up inactive sessions")

        val count = sessionService.cleanupInactiveSessions()

        return ResponseEntity.ok(CleanupResponse(deactivatedCount = count))
    }
}

/**
 * Response DTOs
 */
data class SessionResponse(
    val sessionId: UUID,
    val sessionToken: String,
    val createdAt: String,
    val isActive: Boolean
)

data class ValidationResponse(
    val isValid: Boolean
)

data class CleanupResponse(
    val deactivatedCount: Int
)
