package com.smartpacking.api.exception

import java.util.*

/**
 * Base exception for all custom exceptions in the application
 */
sealed class ApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when a requested resource is not found
 */
class ResourceNotFoundException(
    resourceType: String,
    resourceId: Any
) : ApiException("$resourceType not found with id: $resourceId")

/**
 * Thrown when a session is not found or invalid
 */
class SessionNotFoundException(sessionToken: String) : ApiException("Session not found: $sessionToken")

/**
 * Thrown when a packing list is not found
 */
class PackingListNotFoundException(id: UUID) : ApiException("Packing list not found: $id")

/**
 * Thrown when business validation fails
 */
class ValidationException(message: String) : ApiException(message)

/**
 * Thrown when an external service (like AI Worker) fails
 */
class ExternalServiceException(
    serviceName: String,
    message: String,
    cause: Throwable? = null
) : ApiException("$serviceName failed: $message", cause)
