package com.smartpacking.shared.dto

import com.smartpacking.shared.enums.Season
import com.smartpacking.shared.enums.TravelType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

/**
 * Request to generate a packing list.
 */
data class PackingRequest(
    @field:NotBlank(message = "Destination is required")
    val destination: String,

    @field:Min(value = 1, message = "Duration must be at least 1 day")
    @field:Max(value = 365, message = "Duration cannot exceed 365 days")
    val durationDays: Int,

    val travelType: TravelType,

    val season: Season,

    val travelDate: LocalDate? = null
)
