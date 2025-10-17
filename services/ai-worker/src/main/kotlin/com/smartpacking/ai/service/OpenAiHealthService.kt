package com.smartpacking.ai.service

import org.springframework.ai.chat.client.ChatClient
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service to verify Spring AI OpenAI configuration and connectivity.
 *
 * This service performs basic health checks on the OpenAI service
 * during application startup using Spring AI's ChatClient.
 */
@Service
class OpenAiHealthService(
    private val chatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(OpenAiHealthService::class.java)

    /**
     * Performs basic health check on OpenAI service during startup.
     * Verifies that the ChatClient is properly initialized and can connect to OpenAI.
     */
    @PostConstruct
    fun checkOpenAiConnection() {
        logger.info("Starting Spring AI OpenAI health check...")

        try {
            logger.info("✓ ChatClient bean initialized successfully")

            // Attempt a simple test call to verify API connectivity
            // Using a minimal prompt to minimize token usage
            try {
                val response = chatClient.prompt()
                    .user("Say 'OK' if you can read this.")
                    .call()
                    .content()

                if (response != null) {
                    logger.info("✓ Successfully connected to OpenAI API")
                    logger.info("✓ Test response received: ${response.take(50)}${if (response.length > 50) "..." else ""}")
                    logger.info("✓ Connection test PASSED")
                } else {
                    logger.warn("⚠️  Received null response from OpenAI API")
                }
            } catch (e: Exception) {
                logger.warn("⚠️  Could not connect to OpenAI API (this is expected with placeholder key)")
                logger.warn("⚠️  Error: ${e.message}")
                logger.info("✓ Configuration test PASSED (bean created), but API connection failed")
                logger.info("→ This is normal if using placeholder API key. Replace with real key to enable API calls.")
            }
        } catch (e: Exception) {
            logger.error("✗ OpenAI health check failed", e)
            // Don't throw exception to allow app to start even without valid API key
            logger.warn("→ Application will start, but AI features will not work without valid API key")
        }

        logger.info("Spring AI OpenAI health check completed")
    }

    /**
     * Checks if OpenAI service is available for use.
     *
     * @return true if service is configured and potentially functional
     */
    fun isServiceAvailable(): Boolean {
        return try {
            chatClient != null
        } catch (e: Exception) {
            logger.error("Error checking OpenAI service availability", e)
            false
        }
    }
}
