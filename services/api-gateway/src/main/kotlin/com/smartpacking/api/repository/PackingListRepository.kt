package com.smartpacking.api.repository

import com.smartpacking.api.entity.PackingList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PackingListRepository : JpaRepository<PackingList, UUID> {

    /**
     * Find all packing lists for a given session
     */
    fun findBySessionId(sessionId: UUID): List<PackingList>

    /**
     * Find packing lists by destination
     */
    fun findByDestinationContainingIgnoreCase(destination: String): List<PackingList>

    /**
     * Find packing lists by travel type
     */
    fun findByTravelType(travelType: String): List<PackingList>

    /**
     * Find packing lists by session and destination
     */
    fun findBySessionIdAndDestination(sessionId: UUID, destination: String): List<PackingList>

    /**
     * Find the most recent packing lists (for a specific session or globally)
     */
    @Query("""
        SELECT pl FROM PackingList pl
        WHERE pl.session.id = :sessionId
        ORDER BY pl.createdAt DESC
    """)
    fun findRecentBySession(@Param("sessionId") sessionId: UUID): List<PackingList>

    /**
     * Count packing lists for a session
     */
    fun countBySessionId(sessionId: UUID): Long
}
