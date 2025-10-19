package com.smartpacking.ai.controller

import com.smartpacking.ai.dto.ErrorResponse
import com.smartpacking.ai.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * Global exception handler for AI Worker REST API.
 *
 * Maps custom exceptions to appropriate HTTP status codes and error responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handles validation errors (400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation error: {}", ex.message)

        val errors = ex.bindingResult.allErrors.map { error ->
            when (error) {
                is FieldError -> "${error.field}: ${error.defaultMessage}"
                else -> error.defaultMessage ?: "Validation error"
            }
        }

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Request validation failed",
            path = request.requestURI,
            details = errors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * Handles authentication errors (401 Unauthorized).
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Authentication error: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Authentication Failed",
            message = ex.message ?: "AI service authentication failed",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    /**
     * Handles invalid JSON responses from AI (422 Unprocessable Entity).
     */
    @ExceptionHandler(InvalidJsonResponseException::class)
    fun handleInvalidJsonResponse(
        ex: InvalidJsonResponseException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Invalid JSON response: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
            error = "Invalid AI Response",
            message = ex.message ?: "AI returned invalid response format",
            path = request.requestURI,
            details = if (ex.rawResponse != null) listOf("Raw response available in logs") else null
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse)
    }

    /**
     * Handles validation errors (422 Unprocessable Entity).
     */
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(
        ex: ValidationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Response validation failed: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
            error = "Response Validation Failed",
            message = ex.message ?: "AI response failed validation",
            path = request.requestURI,
            details = ex.validationErrors.ifEmpty { null }
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse)
    }

    /**
     * Handles rate limit errors (429 Too Many Requests).
     */
    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(
        ex: RateLimitException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Rate limit exceeded: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            error = "Rate Limit Exceeded",
            message = ex.message ?: "AI service rate limit exceeded",
            path = request.requestURI,
            details = ex.retryAfterSeconds?.let { listOf("Retry after $it seconds") }
        )

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse)
    }

    /**
     * Handles timeout errors (504 Gateway Timeout).
     */
    @ExceptionHandler(AiTimeoutException::class)
    fun handleTimeoutException(
        ex: AiTimeoutException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("AI service timeout: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.GATEWAY_TIMEOUT.value(),
            error = "Service Timeout",
            message = ex.message ?: "AI service request timed out",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(errorResponse)
    }

    /**
     * Handles service unavailable errors (503 Service Unavailable).
     */
    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleServiceUnavailable(
        ex: ServiceUnavailableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("AI service unavailable: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "Service Unavailable",
            message = ex.message ?: "AI service is currently unavailable",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }

    /**
     * Handles generic AI service errors (500 Internal Server Error).
     */
    @ExceptionHandler(AiServiceException::class)
    fun handleAiServiceException(
        ex: AiServiceException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("AI service error: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "AI Service Error",
            message = ex.message ?: "An error occurred in the AI service",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * Handles all other unexpected errors (500 Internal Server Error).
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
