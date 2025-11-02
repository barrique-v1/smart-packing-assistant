package com.smartpacking.ai.config

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

/**
 * Configuration class for Spring AI OpenAI integration.
 *
 * Spring AI auto-configures the OpenAI client based on application.properties.
 * This class validates the API key and creates a ChatClient bean for easier usage.
 */
@Configuration
class OpenAiConfig {

    private val logger = LoggerFactory.getLogger(OpenAiConfig::class.java)

    @Value("\${spring.ai.openai.api-key}")
    private lateinit var apiKey: String

    @Value("\${spring.ai.openai.chat.options.model}")
    private lateinit var model: String

    @Value("\${spring.ai.openai.chat.options.temperature}")
    private var temperature: Double = 0.3

    @Value("\${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private lateinit var embeddingModel: String

    /**
     * Validates API key configuration on startup.
     */
    @PostConstruct
    fun validateConfiguration() {
        logger.info("Initializing Spring AI with OpenAI")
        logger.info("Model: $model, Temperature: $temperature")

        when {
            apiKey.isBlank() -> {
                logger.error("OpenAI API key is not configured!")
                throw IllegalStateException("OpenAI API key must be configured. Set OPENAI_API_KEY environment variable.")
            }
            apiKey.contains("placeholder") -> {
                logger.warn("⚠️  Using placeholder API key! Replace with real key from instructor")
                logger.warn("⚠️  Set environment variable: export OPENAI_API_KEY=your_key_here")
            }
            apiKey.startsWith("sk-") -> {
                logger.info("✓ OpenAI API key configured (starts with 'sk-')")
            }
            else -> {
                logger.warn("API key format looks unusual. Expected format: sk-...")
            }
        }
    }

    /**
     * Creates a ChatClient bean for simplified AI interactions.
     *
     * ChatClient provides a fluent API for building prompts and processing responses.
     * Spring Boot auto-configures the underlying OpenAiChatModel.
     */
    @Bean
    fun chatClient(chatModel: OpenAiChatModel): ChatClient {
        return ChatClient.builder(chatModel).build()
    }

    /**
     * Spring AI auto-configures OpenAiEmbeddingModel based on application.properties.
     *
     * Configuration properties:
     * - spring.ai.openai.embedding.options.model=text-embedding-3-small
     * - spring.ai.openai.embedding.options.dimensions=1536
     *
     * The auto-configured bean is used by VectorSearchService to generate embeddings
     * for user queries when searching the Qdrant vector database.
     *
     * No explicit @Bean needed - Spring Boot handles this automatically!
     */

    /**
     * Returns the configured model name for logging/debugging purposes.
     */
    fun getModel(): String = model

    /**
     * Returns the configured temperature for logging/debugging purposes.
     */
    fun getTemperature(): Double = temperature
}
