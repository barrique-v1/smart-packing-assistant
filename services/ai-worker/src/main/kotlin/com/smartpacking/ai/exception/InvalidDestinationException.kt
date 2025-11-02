package com.smartpacking.ai.exception

/**
 * Exception thrown when a user provides an invalid destination.
 *
 * This exception is part of the anti-hallucination strategy, ensuring
 * that only validated destinations are processed by the AI system.
 *
 * @property destination The invalid destination provided by the user
 * @property validDestinations Complete list of valid destinations (for API response)
 */
class InvalidDestinationException(
    val destination: String,
    val validDestinations: List<String> = emptyList()
) : RuntimeException(buildMessage(destination)) {

    companion object {
        /**
         * Build a clear, user-friendly error message.
         *
         * @param destination The invalid destination
         * @return Error message explaining the issue
         */
        private fun buildMessage(destination: String): String {
            return "Invalid destination: '$destination'. " +
                "Please choose from our supported destinations. " +
                "Use GET /api/ai/destinations to see the full list."
        }
    }
}
