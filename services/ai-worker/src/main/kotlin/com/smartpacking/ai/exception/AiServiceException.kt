package com.smartpacking.ai.exception

/**
 * Base exception for all AI service errors.
 */
open class AiServiceException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Thrown when the AI API call times out.
 */
class AiTimeoutException(
    message: String = "AI service request timed out",
    cause: Throwable? = null
) : AiServiceException(message, cause)

/**
 * Thrown when the AI API returns invalid or unparseable JSON.
 */
class InvalidJsonResponseException(
    message: String,
    val rawResponse: String? = null,
    cause: Throwable? = null
) : AiServiceException(message, cause)

/**
 * Thrown when the AI API rate limit is exceeded.
 */
class RateLimitException(
    message: String = "AI service rate limit exceeded",
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null
) : AiServiceException(message, cause)

/**
 * Thrown when the AI response fails validation.
 */
class ValidationException(
    message: String,
    val validationErrors: List<String> = emptyList(),
    cause: Throwable? = null
) : AiServiceException(message, cause)

/**
 * Thrown when the AI API authentication fails.
 */
class AuthenticationException(
    message: String = "AI service authentication failed - check API key",
    cause: Throwable? = null
) : AiServiceException(message, cause)

/**
 * Thrown when the AI service is unavailable.
 */
class ServiceUnavailableException(
    message: String = "AI service is currently unavailable",
    cause: Throwable? = null
) : AiServiceException(message, cause)
