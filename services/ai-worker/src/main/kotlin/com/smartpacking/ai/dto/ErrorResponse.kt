package com.smartpacking.ai.dto

import java.time.LocalDateTime

/**
 * Standard error response for API endpoints.
 *
 * Provides consistent error format across all AI Worker endpoints.
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String? = null,
    val details: List<String>? = null
)
