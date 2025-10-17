package com.smartpacking.ai.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data class representing a cultural tip for a specific location.
 * This mirrors the structure in culture_tips.json.
 */
data class CultureTip(
    @JsonProperty("category")
    val category: String,  // DRESS_CODE, CUSTOMS, LANGUAGE, SAFETY

    @JsonProperty("tip")
    val tip: String,

    @JsonProperty("importance")
    val importance: String  // LOW, MEDIUM, HIGH
)

/**
 * Container for all culture tips organized by location.
 * Key = Location name (e.g., "Dubai", "Iceland", "Tokyo")
 * Value = List of CultureTip
 */
typealias CultureTipsMap = Map<String, List<CultureTip>>
