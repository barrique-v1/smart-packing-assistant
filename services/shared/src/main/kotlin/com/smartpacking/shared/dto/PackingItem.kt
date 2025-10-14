package com.smartpacking.shared.dto

/**
 * Individual item in a packing list.
 */
data class PackingItem(
    val item: String,
    val quantity: Int = 1,
    val reason: String? = null
)
