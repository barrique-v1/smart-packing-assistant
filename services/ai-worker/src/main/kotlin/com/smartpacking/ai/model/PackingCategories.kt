package com.smartpacking.ai.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents the categorized items in a packing list.
 *
 * This model matches the "categories" structure from the AI response:
 * {
 *   "clothing": [...],
 *   "tech": [...],
 *   "hygiene": [...],
 *   "documents": [...],
 *   "other": [...]
 * }
 */
data class PackingCategories(
    @JsonProperty("clothing")
    val clothing: List<PackingItem> = emptyList(),

    @JsonProperty("tech")
    val tech: List<PackingItem> = emptyList(),

    @JsonProperty("hygiene")
    val hygiene: List<PackingItem> = emptyList(),

    @JsonProperty("documents")
    val documents: List<PackingItem> = emptyList(),

    @JsonProperty("other")
    val other: List<PackingItem> = emptyList()
) {
    /**
     * Returns the total number of items across all categories.
     */
    fun getTotalItemCount(): Int {
        return clothing.size + tech.size + hygiene.size + documents.size + other.size
    }

    /**
     * Returns the total quantity across all items.
     */
    fun getTotalQuantity(): Int {
        return (clothing + tech + hygiene + documents + other).sumOf { it.quantity }
    }

    /**
     * Validates that the packing list has at least some items.
     */
    fun validate() {
        require(getTotalItemCount() > 0) {
            "Packing list must contain at least one item"
        }
    }

    /**
     * Returns all items as a flat list.
     */
    fun getAllItems(): List<PackingItem> {
        return clothing + tech + hygiene + documents + other
    }
}
