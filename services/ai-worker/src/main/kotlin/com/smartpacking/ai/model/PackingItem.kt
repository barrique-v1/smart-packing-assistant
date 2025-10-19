package com.smartpacking.ai.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a single item in the packing list.
 *
 * This model matches the JSON structure returned by the AI:
 * {"item": "T-Shirt", "quantity": 3, "reason": "Hot weather, change daily"}
 */
data class PackingItem(
    @JsonProperty("item")
    val item: String,

    @JsonProperty("quantity")
    val quantity: Int,

    @JsonProperty("reason")
    val reason: String
) {
    init {
        require(item.isNotBlank()) { "Item name cannot be blank" }
        require(quantity > 0) { "Quantity must be positive, got: $quantity" }
        require(reason.isNotBlank()) { "Reason cannot be blank" }
    }
}
