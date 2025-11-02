package com.smartpacking.ai.service

import com.smartpacking.shared.dto.PackingRequest
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

// Qdrant API Data Classes
data class QdrantSearchRequest(
    val vector: List<Float>,
    val limit: Int,
    val scoreThreshold: Double? = null,
    val filter: QdrantFilter? = null,
    val withPayload: Boolean = true,
    val withVector: Boolean = false
)

data class QdrantFilter(
    val must: List<QdrantCondition>? = null,
    val should: List<QdrantCondition>? = null
)

data class QdrantCondition(
    val key: String,
    val match: QdrantMatch? = null
)

data class QdrantMatch(
    val value: Any? = null,
    val any: List<Any>? = null
)

data class QdrantSearchResponse(
    val result: List<QdrantPoint>,
    val status: String,
    val time: Double
)

data class QdrantPoint(
    val id: String,
    val score: Double,
    val payload: Map<String, Any>,
    val vector: List<Float>? = null
)

data class RetrievedItem(
    val item: String,
    val category: String,
    val quantity: Int,
    val reason: String,
    val score: Double,
    val importance: String,
    val tags: List<String>
)

/**
 * Service for vector similarity search using Qdrant vector database.
 *
 * This service implements RAG (Retrieval-Augmented Generation) by:
 * 1. Converting user queries to embeddings via OpenAI text-embedding-3-small
 * 2. Searching Qdrant for similar expert-verified packing items
 * 3. Filtering results by travel type, season, and confidence score
 * 4. Returning top-K items to enhance GPT prompts
 *
 * Graceful degradation: Returns empty list on failure, allowing pure GPT fallback.
 */
@Service
class VectorSearchService(
    private val restTemplate: RestTemplate,
    private val embeddingModel: EmbeddingModel,
    @Value("\${qdrant.url}") private val qdrantUrl: String,
    @Value("\${qdrant.collection}") private val collection: String,
    @Value("\${qdrant.min-score}") private val minScore: Double,
    @Value("\${qdrant.top-k}") private val topK: Int
) {
    private val logger = LoggerFactory.getLogger(VectorSearchService::class.java)

    /**
     * Search for relevant packing items based on destination and context.
     *
     * Process:
     * - Build contextual query from request (destination, travel type, season, duration)
     * - Generate embedding vector (1536 dimensions)
     * - Filter by travel_type (exact) and season (any match)
     * - Return top-K items with score >= min-score threshold
     *
     * @param request Packing request with trip parameters
     * @return List of retrieved items sorted by relevance score (descending)
     */
    fun searchRelevantItems(request: PackingRequest): List<RetrievedItem> {
        try {
            // 1. Create query text from request context
            val queryText = buildQueryText(request)
            logger.info("Searching with query: $queryText")

            // 2. Generate embedding for query
            val queryEmbedding = generateEmbedding(queryText)
            logger.debug("Generated embedding with ${queryEmbedding.size} dimensions")

            // 3. Build filter based on request parameters
            val filter = buildFilter(request)

            // 4. Execute search in Qdrant
            val searchRequest = QdrantSearchRequest(
                vector = queryEmbedding,
                limit = topK,
                scoreThreshold = minScore,
                filter = filter,
                withPayload = true,
                withVector = false
            )

            val url = "$qdrantUrl/collections/$collection/points/search"
            val response = restTemplate.postForObject(url, searchRequest, QdrantSearchResponse::class.java)

            val items = response?.result?.map { point ->
                RetrievedItem(
                    item = point.payload["item"] as String,
                    category = point.payload["category"] as String,
                    quantity = (point.payload["quantity"] as Number).toInt(),
                    reason = point.payload["reason"] as String,
                    score = point.score,
                    importance = point.payload["importance"] as? String ?: "medium",
                    tags = (point.payload["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            } ?: emptyList()

            logger.info("Retrieved ${items.size} items from knowledge base (score >= $minScore)")
            if (items.isNotEmpty()) {
                logger.info("Top scores: ${items.take(5).map { String.format("%.3f", it.score) }}")
            }

            return items

        } catch (e: Exception) {
            logger.error("Vector search failed: ${e.message}", e)
            // Return empty list on failure - will fall back to GPT generation
            return emptyList()
        }
    }

    /**
     * Build contextual query text from packing request.
     *
     * Includes:
     * - Destination, travel type, duration, season
     * - Contextual keywords for travel type (e.g., "formal attire" for business)
     * - Weather-related keywords (e.g., "sun protection" for summer)
     *
     * This rich context improves embedding quality and retrieval accuracy.
     */
    private fun buildQueryText(request: PackingRequest): String {
        return buildString {
            append("Packing for trip to ${request.destination}. ")
            append("Travel type: ${request.travelType}. ")
            append("Duration: ${request.durationDays} days. ")
            append("Season: ${request.season}. ")

            // Add contextual keywords for better matching
            when (request.travelType.toString()) {
                "BUSINESS" -> append("Professional meetings, formal attire, work equipment. ")
                "VACATION" -> append("Leisure activities, relaxation, tourism. ")
                "BACKPACKING" -> append("Adventure travel, hiking, camping, budget travel. ")
            }

            when (request.season.toString()) {
                "SUMMER" -> append("Hot weather, sun protection, light clothing. ")
                "WINTER" -> append("Cold weather, insulation, warm layers. ")
                "SPRING" -> append("Mild weather, rain protection, layers. ")
                "FALL" -> append("Cool weather, transitional clothing, wind protection. ")
            }
        }
    }

    /**
     * Generate embedding vector using OpenAI text-embedding-3-small.
     *
     * Model: text-embedding-3-small (1536 dimensions)
     * Configured via: spring.ai.openai.embedding.options.model
     *
     * @param text Query text to embed
     * @return Embedding vector as List<Float>
     * @throws RuntimeException if embedding generation fails
     */
    private fun generateEmbedding(text: String): List<Float> {
        try {
            val embeddingRequest = EmbeddingRequest(listOf(text), null)
            val embeddingResponse = embeddingModel.call(embeddingRequest)

            return embeddingResponse.results
                .firstOrNull()
                ?.output
                ?.map { it.toFloat() }
                ?: throw IllegalStateException("No embedding returned from API")

        } catch (e: Exception) {
            logger.error("Embedding generation failed: ${e.message}", e)
            throw RuntimeException("Failed to generate embedding", e)
        }
    }

    /**
     * Build Qdrant filter based on request parameters.
     *
     * Filters:
     * - travel_type: Exact match (BUSINESS/VACATION/BACKPACKING)
     * - season: Array contains (requested season OR "all" for universal items)
     *
     * These filters ensure retrieved items are contextually appropriate.
     */
    private fun buildFilter(request: PackingRequest): QdrantFilter {
        val conditions = mutableListOf<QdrantCondition>()

        // Filter by travel type (exact match)
        conditions.add(
            QdrantCondition(
                key = "travel_type",
                match = QdrantMatch(value = request.travelType.toString())
            )
        )

        // Filter by season (array contains)
        conditions.add(
            QdrantCondition(
                key = "season",
                match = QdrantMatch(any = listOf(request.season.toString(), "all"))
            )
        )

        return QdrantFilter(must = conditions)
    }

    /**
     * Check Qdrant health and connectivity.
     *
     * @return true if Qdrant is accessible, false otherwise
     */
    fun checkHealth(): Boolean {
        return try {
            val url = "$qdrantUrl/healthz"
            val response = restTemplate.getForObject(url, String::class.java)
            logger.info("Qdrant health check: $response")
            true
        } catch (e: Exception) {
            logger.error("Qdrant health check failed: ${e.message}")
            false
        }
    }

    /**
     * Get knowledge base statistics.
     *
     * @return Collection metadata including vector count, dimensions, etc.
     */
    @Suppress("UNCHECKED_CAST")
    fun getCollectionStats(): Map<String, Any> {
        return try {
            val url = "$qdrantUrl/collections/$collection"
            val response = restTemplate.getForObject(url, Map::class.java)
            (response as? Map<String, Any>) ?: emptyMap()
        } catch (e: Exception) {
            logger.error("Failed to get collection stats: ${e.message}")
            emptyMap()
        }
    }
}
