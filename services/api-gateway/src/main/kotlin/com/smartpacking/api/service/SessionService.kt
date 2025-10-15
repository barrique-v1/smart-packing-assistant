package com.smartpacking.api.service

import com.smartpacking.api.entity.Session
import com.smartpacking.api.exception.SessionNotFoundException
import com.smartpacking.api.repository.SessionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

/**
 * Service for managing user sessions
 */
@Service
@Transactional
class SessionService(
    private val sessionRepository: SessionRepository
) {

    private val logger = LoggerFactory.getLogger(SessionService::class.java)
    private val secureRandom = SecureRandom()

    /**
     * Create a new session with a unique token
     */
    fun createSession(): Session {
        val sessionToken = generateSecureToken()
        val session = Session(sessionToken = sessionToken)

        val savedSession = sessionRepository.save(session)
        logger.info("Created new session: ${savedSession.id}")

        return savedSession
    }

    /**
     * Find an active session by token
     */
    fun findByToken(token: String): Session {
        return sessionRepository.findBySessionTokenAndIsActiveTrue(token)
            ?: throw SessionNotFoundException(token)
    }

    /**
     * Update last activity timestamp for a session
     */
    fun updateActivity(token: String) {
        val count = sessionRepository.updateLastActivity(token, LocalDateTime.now())
        if (count == 0) {
            logger.warn("Failed to update activity for session: $token")
        }
    }

    /**
     * Deactivate old sessions (inactive for 24+ hours)
     */
    fun cleanupInactiveSessions(): Int {
        val cutoffTime = LocalDateTime.now().minusHours(24)
        val count = sessionRepository.deactivateInactiveSessions(cutoffTime)

        if (count > 0) {
            logger.info("Deactivated $count inactive sessions")
        }

        return count
    }

    /**
     * Generate a cryptographically secure session token
     */
    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Validate if a session exists and is active
     */
    fun validateSession(token: String): Boolean {
        return sessionRepository.findBySessionTokenAndIsActiveTrue(token) != null
    }

    /**
     * Get all sessions (for admin/debugging purposes)
     */
    @Transactional(readOnly = true)
    fun getAllSessions(): List<Session> {
        return sessionRepository.findAll()
    }

    /**
     * Get all active sessions
     */
    @Transactional(readOnly = true)
    fun getActiveSessions(): List<Session> {
        return sessionRepository.findByIsActiveTrue()
    }
}
