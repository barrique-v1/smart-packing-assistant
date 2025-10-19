package com.smartpacking.ai.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Top-level response model from the AI service.
 *
 * This model matches the complete JSON structure returned by OpenAI:
 * {
 *   "categories": {
 *     "clothing": [...],
 *     "tech": [...],
 *     ...
 *   }
 * }
 */
data class AiPackingResponse(
    @JsonProperty("categories")
    val categories: PackingCategories
) {
    /**
     * Validates the entire response structure.
     */
    fun validate() {
        categories.validate()
    }

    /**
     * Returns a human-readable summary of the packing list.
     */
    fun getSummary(): String {
        return """
            Packing List Summary:
            - Clothing items: ${categories.clothing.size}
            - Tech items: ${categories.tech.size}
            - Hygiene items: ${categories.hygiene.size}
            - Documents: ${categories.documents.size}
            - Other items: ${categories.other.size}
            Total: ${categories.getTotalItemCount()} items (${categories.getTotalQuantity()} pieces)
        """.trimIndent()
    }
}
