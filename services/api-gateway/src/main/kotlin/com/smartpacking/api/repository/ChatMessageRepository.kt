package com.smartpacking.api.repository

import com.smartpacking.api.entity.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {

    /**
     * Find all chat messages for a specific packing list
     */
    fun findByPackingListIdOrderByCreatedAtAsc(packingListId: UUID): List<ChatMessage>

    /**
     * Count chat messages for a packing list
     */
    fun countByPackingListId(packingListId: UUID): Long
}
