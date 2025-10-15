package com.smartpacking.api.service

import com.smartpacking.api.exception.ExternalServiceException
import com.smartpacking.shared.dto.PackingRequest
import com.smartpacking.shared.dto.PackingResponse
import com.smartpacking.shared.dto.PackingItem
import com.smartpacking.shared.model.WeatherInfo
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
 * Mock implementation of AI Worker Client
 * Returns realistic dummy data for testing
 * Will be replaced with real HTTP client when AI Worker is ready
 */
@Service
class MockAiWorkerClient(
    @Value("\${ai.worker.url:http://localhost:8081}")
    private val aiWorkerUrl: String,
    private val restTemplate: RestTemplate
) : AiWorkerClient {

    private val logger = LoggerFactory.getLogger(MockAiWorkerClient::class.java)

    override fun generatePackingList(request: PackingRequest): PackingResponse {
        logger.info("Mock AI Worker: Generating packing list for ${request.destination}")

        // TODO: Replace with actual HTTP call when AI Worker is ready
        // val response = restTemplate.postForObject(
        //     "$aiWorkerUrl/api/ai/generate",
        //     request,
        //     PackingResponse::class.java
        // ) ?: throw ExternalServiceException("AI Worker", "No response received")

        return generateMockResponse(request)
    }

    private fun generateMockResponse(request: PackingRequest): PackingResponse {
        val categories = when (request.travelType.name) {
            "BUSINESS" -> generateBusinessCategories(request)
            "VACATION" -> generateVacationCategories(request)
            "BACKPACKING" -> generateBackpackingCategories(request)
            else -> generateVacationCategories(request)
        }

        val weatherInfo = generateWeatherInfo(request)
        val cultureTips = generateCultureTips(request.destination)

        return PackingResponse(
            id = java.util.UUID.randomUUID(), // Temporary, will be replaced
            destination = request.destination,
            categories = categories,
            weatherInfo = weatherInfo,
            cultureTips = cultureTips
        )
    }

    private fun generateBusinessCategories(request: PackingRequest): com.smartpacking.shared.dto.PackingCategories {
        return com.smartpacking.shared.dto.PackingCategories(
            clothing = listOf(
                PackingItem("Business suit", 2, "Professional attire for meetings"),
                PackingItem("Dress shirts", 3, "White and light blue"),
                PackingItem("Dress shoes", 1, "Black leather")
            ),
            tech = listOf(
                PackingItem("Laptop", 1, "For presentations and work"),
                PackingItem("Laptop charger", 1, "Essential for business trip"),
                PackingItem("Travel adapter", 1, "Universal adapter for ${request.destination}")
            ),
            hygiene = listOf(
                PackingItem("Toiletries", 1, "Basic hygiene items")
            ),
            documents = listOf(
                PackingItem("Business cards", 1, "Networking essential"),
                PackingItem("Passport", 1, "Required for international travel")
            ),
            other = listOf(
                PackingItem("Notepad", 1, "For taking notes in meetings")
            )
        )
    }

    private fun generateVacationCategories(request: PackingRequest): com.smartpacking.shared.dto.PackingCategories {
        val durationDays = request.durationDays
        return com.smartpacking.shared.dto.PackingCategories(
            clothing = listOf(
                PackingItem("T-Shirts", (durationDays / 2).coerceAtLeast(3), "Casual wear"),
                PackingItem("Shorts", 2, "For warm weather"),
                PackingItem("Jeans", 1, "Versatile casual wear"),
                PackingItem("Jacket", 1, "For cooler evenings"),
                PackingItem("Underwear", durationDays + 2, "Bring extra"),
                PackingItem("Socks", durationDays, "One pair per day")
            ),
            tech = listOf(
                PackingItem("Smartphone", 1, "For photos and navigation"),
                PackingItem("Charger", 1, "Phone charger"),
                PackingItem("Camera", 1, "Capture memories")
            ),
            hygiene = listOf(
                PackingItem("Sunscreen", 1, "SPF 50+ recommended"),
                PackingItem("Toothbrush", 1, "Dental hygiene"),
                PackingItem("Toothpaste", 1, "Travel size")
            ),
            documents = listOf(
                PackingItem("Passport", 1, "Essential travel document"),
                PackingItem("Travel insurance", 1, "Keep a copy")
            ),
            other = listOf(
                PackingItem("Sunglasses", 1, "Sun protection")
            )
        )
    }

    private fun generateBackpackingCategories(request: PackingRequest): com.smartpacking.shared.dto.PackingCategories {
        return com.smartpacking.shared.dto.PackingCategories(
            clothing = listOf(
                PackingItem("Hiking boots", 1, "Waterproof and broken in"),
                PackingItem("Quick-dry shirts", 3, "Lightweight and packable"),
                PackingItem("Hiking pants", 2, "Convertible to shorts"),
                PackingItem("Rain jacket", 1, "Waterproof and breathable")
            ),
            tech = listOf(
                PackingItem("Headlamp", 1, "With extra batteries"),
                PackingItem("Power bank", 1, "10000mAh capacity")
            ),
            hygiene = listOf(
                PackingItem("First aid kit", 1, "Including blister treatment")
            ),
            documents = listOf(
                PackingItem("Maps", 1, "Offline backup for navigation"),
                PackingItem("Passport", 1, "Essential document")
            ),
            other = listOf(
                PackingItem("Sleeping bag", 1, "Appropriate for season"),
                PackingItem("Backpack", 1, "40-60L capacity"),
                PackingItem("Water bottle", 1, "Reusable, 1L capacity"),
                PackingItem("Multi-tool", 1, "Swiss Army knife style")
            )
        )
    }

    private fun generateWeatherInfo(request: PackingRequest): WeatherInfo {
        // Mock weather data based on destination
        return when (request.destination.lowercase()) {
            "dubai" -> WeatherInfo(
                tempMin = 25,
                tempMax = 40,
                conditions = "Hot and sunny",
                humidity = "Low",
                precipitationChance = 5
            )
            "iceland" -> WeatherInfo(
                tempMin = 2,
                tempMax = 12,
                conditions = "Cool with frequent rain",
                humidity = "High",
                precipitationChance = 70
            )
            "tokyo" -> WeatherInfo(
                tempMin = 15,
                tempMax = 25,
                conditions = "Mild and pleasant",
                humidity = "Moderate",
                precipitationChance = 40
            )
            else -> WeatherInfo(
                tempMin = 15,
                tempMax = 25,
                conditions = "Variable",
                humidity = "Moderate",
                precipitationChance = 50
            )
        }
    }

    private fun generateCultureTips(destination: String): List<String> {
        return when (destination.lowercase()) {
            "dubai" -> listOf(
                "Dress modestly - shoulders and knees should be covered in public areas",
                "Alcohol is only permitted in licensed hotels",
                "Public displays of affection are not allowed"
            )
            "iceland" -> listOf(
                "Weather can change rapidly - always pack layers",
                "Remove shoes before entering homes",
                "English is widely spoken"
            )
            "tokyo" -> listOf(
                "Tipping is not customary and can be considered rude",
                "Speak quietly on public transportation",
                "Shoes are often removed when entering homes"
            )
            else -> listOf(
                "Research local customs before traveling",
                "Learn a few basic phrases in the local language",
                "Be respectful of cultural differences"
            )
        }
    }
}
