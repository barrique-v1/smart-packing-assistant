package com.smartpacking.shared.dto

import com.smartpacking.shared.model.WeatherInfo
import java.util.UUID

/**
 * Response containing a generated packing list.
 */
data class PackingResponse(
    val id: UUID,
    val destination: String,
    val categories: PackingCategories,
    val weatherInfo: WeatherInfo? = null,
    val cultureTips: List<String> = emptyList(),
    val specialNotes: String? = null
)

/**
 * Categorized packing items.
 */
data class PackingCategories(
    val clothing: List<PackingItem> = emptyList(),
    val tech: List<PackingItem> = emptyList(),
    val hygiene: List<PackingItem> = emptyList(),
    val documents: List<PackingItem> = emptyList(),
    val other: List<PackingItem> = emptyList()
)
