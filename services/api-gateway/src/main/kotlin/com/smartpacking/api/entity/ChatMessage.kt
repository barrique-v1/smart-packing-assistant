package com.smartpacking.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "chat_messages")
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "packing_list_id", nullable = false)
    val packingList: PackingList,

    // Message Content
    @Column(name = "role", nullable = false, length = 20)
    val role: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,

    // Metadata
    @Column(name = "ai_model", length = 100)
    val aiModel: String? = null,

    @Column(name = "tokens_used")
    val tokensUsed: Int? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatMessage) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "ChatMessage(id=$id, role='$role', content='${content.take(50)}...')"
    }
}
