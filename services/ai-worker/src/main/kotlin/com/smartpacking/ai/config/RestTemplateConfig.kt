package com.smartpacking.ai.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Configuration for RestTemplate used in Qdrant HTTP API calls.
 *
 * RestTemplate is used by VectorSearchService to communicate with
 * Qdrant vector database via REST API.
 */
@Configuration
class RestTemplateConfig {

    /**
     * Creates a configured RestTemplate bean for HTTP calls.
     *
     * Timeouts:
     * - Connect timeout: 5 seconds (connection establishment)
     * - Read timeout: 10 seconds (waiting for response)
     *
     * These timeouts are suitable for local Qdrant instances and
     * should be adjusted for production deployments if needed.
     */
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build()
    }
}
