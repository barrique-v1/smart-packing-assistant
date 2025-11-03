package com.smartpacking.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.smartpacking.api.exception.ExternalServiceException
import com.smartpacking.shared.dto.PackingRequest
import com.smartpacking.shared.dto.PackingResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

/**
 * Client for communicating with AI Worker service
 */
interface AiWorkerClient {
    fun generatePackingList(request: PackingRequest): PackingResponse
}

/**
 * Real implementation of AI Worker Client.
 * Communicates with AI Worker service via HTTP.
 *
 * Error handling:
 * - Connection failures (service down)
 * - Client errors (400-level)
 * - Server errors (500-level)
 * - Unexpected exceptions
 */
@Service
class RealAiWorkerClient(
    @Value("\${ai.worker.url:http://localhost:8081}")
    private val aiWorkerUrl: String,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) : AiWorkerClient {

    private val logger = LoggerFactory.getLogger(RealAiWorkerClient::class.java)

    override fun generatePackingList(request: PackingRequest): PackingResponse {
        logger.info("Calling AI Worker: Generating packing list for ${request.destination}")
        logger.debug("AI Worker URL: $aiWorkerUrl/api/ai/generate")

        return try {
            val response = restTemplate.postForObject(
                "$aiWorkerUrl/api/ai/generate",
                request,
                PackingResponse::class.java
            ) ?: throw ExternalServiceException("AI Worker", "No response received from AI Worker")

            logger.info("✓ Successfully received packing list from AI Worker")
            logger.debug("Response contains ${getTotalItems(response)} items")

            response

        } catch (e: org.springframework.web.client.ResourceAccessException) {
            logger.error("✗ AI Worker connection failed: ${e.message}")
            throw ExternalServiceException(
                "AI Worker",
                "Could not connect to AI Worker at $aiWorkerUrl. Is the service running?",
                e
            )
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            val errorMessage = extractErrorMessage(e)
            logger.error("✗ AI Worker returned client error (${e.statusCode}): $errorMessage")
            throw ExternalServiceException(
                "AI Worker",
                errorMessage,
                e
            )
        } catch (e: org.springframework.web.client.HttpServerErrorException) {
            val errorMessage = extractErrorMessage(e)
            logger.error("✗ AI Worker returned server error (${e.statusCode}): $errorMessage")
            throw ExternalServiceException(
                "AI Worker",
                errorMessage,
                e
            )
        } catch (e: ExternalServiceException) {
            // Re-throw our own exceptions
            throw e
        } catch (e: Exception) {
            logger.error("✗ Unexpected error calling AI Worker: ${e.message}", e)
            throw ExternalServiceException(
                "AI Worker",
                "Unexpected error communicating with AI Worker: ${e.message}",
                e
            )
        }
    }

    /**
     * Extract the actual error message from AI Worker error response.
     *
     * Parses the JSON error response to get the detailed message instead of just the HTTP status text.
     * Falls back to status text if parsing fails.
     */
    private fun extractErrorMessage(e: org.springframework.web.client.HttpStatusCodeException): String {
        return try {
            val responseBody = e.responseBodyAsString
            if (responseBody.isNotBlank()) {
                val errorNode = objectMapper.readTree(responseBody)
                val message = errorNode.get("message")?.asText()

                // Return the detailed message from AI Worker, or fall back to status text
                message ?: e.statusText
            } else {
                e.statusText
            }
        } catch (parseError: Exception) {
            logger.warn("Could not parse error response: ${parseError.message}")
            e.statusText
        }
    }

    /**
     * Helper method to count total items in response for logging.
     */
    private fun getTotalItems(response: PackingResponse): Int {
        return response.categories.clothing.size +
                response.categories.tech.size +
                response.categories.hygiene.size +
                response.categories.documents.size +
                response.categories.other.size
    }
}
