package com.smartpacking.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "packing_lists")
data class PackingList(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    val session: Session,

    // Request Parameters
    @Column(name = "destination", nullable = false)
    val destination: String,

    @Column(name = "duration_days", nullable = false)
    val durationDays: Int,

    @Column(name = "travel_type", nullable = false, length = 50)
    val travelType: String,

    @Column(name = "travel_date")
    val travelDate: LocalDate? = null,

    @Column(name = "season", nullable = false, length = 20)
    val season: String,

    // AI Generated Content
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_json", nullable = false, columnDefinition = "jsonb")
    val itemsJson: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weather_info", columnDefinition = "jsonb")
    val weatherInfo: String? = null,

    @Column(name = "culture_tips", columnDefinition = "TEXT[]")
    val cultureTips: Array<String>? = null,

    @Column(name = "special_notes", columnDefinition = "TEXT")
    val specialNotes: String? = null,

    // Metadata
    @Column(name = "ai_model", length = 100)
    val aiModel: String? = "gpt-4",

    @Column(name = "generation_time_ms")
    val generationTimeMs: Int? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "packingList", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chatMessages: MutableList<ChatMessage> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackingList) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String {
        return "PackingList(id=$id, destination='$destination', travelType='$travelType', season='$season')"
    }
}
