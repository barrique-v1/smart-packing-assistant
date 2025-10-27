package com.smartpacking.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        // Allow credentials (for session tokens in headers)
        config.allowCredentials = true

        // Allow all origins in development (restrict in production!)
        config.addAllowedOriginPattern("*")

        // Allow all headers
        config.addAllowedHeader("*")

        // Allow all HTTP methods
        config.addAllowedMethod("*")

        // Expose custom headers (for session tokens)
        config.addExposedHeader("X-Session-Token")

        // Apply CORS to all endpoints
        source.registerCorsConfiguration("/**", config)

        return CorsFilter(source)
    }
}
