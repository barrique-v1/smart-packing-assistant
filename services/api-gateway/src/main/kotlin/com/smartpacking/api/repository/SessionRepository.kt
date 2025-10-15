package com.smartpacking.api.repository

import com.smartpacking.api.entity.Session
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface SessionRepository : JpaRepository<Session, UUID> {

    /**
     * Find an active session by session token
     */
    fun findBySessionTokenAndIsActiveTrue(sessionToken: String): Session?

    /**
     * Find any session by session token (active or inactive)
     */
    fun findBySessionToken(sessionToken: String): Session?

    /**
     * Find all active sessions
     */
    fun findByIsActiveTrue(): List<Session>

    /**
     * Deactivate sessions that have been inactive for more than 24 hours
     */
    @Modifying
    @Query("""
        UPDATE Session s
        SET s.isActive = false
        WHERE s.lastActivity < :cutoffTime
        AND s.isActive = true
    """)
    fun deactivateInactiveSessions(@Param("cutoffTime") cutoffTime: LocalDateTime): Int

    /**
     * Update session activity timestamp
     */
    @Modifying
    @Query("""
        UPDATE Session s
        SET s.lastActivity = :timestamp
        WHERE s.sessionToken = :sessionToken
    """)
    fun updateLastActivity(
        @Param("sessionToken") sessionToken: String,
        @Param("timestamp") timestamp: LocalDateTime
    ): Int
}
