package com.smartpacking.ai.mapper

import com.smartpacking.ai.model.AiPackingResponse
import com.smartpacking.ai.model.PackingItem
import com.smartpacking.shared.dto.PackingCategories as SharedPackingCategories
import com.smartpacking.shared.dto.PackingItem as SharedPackingItem
import com.smartpacking.shared.dto.PackingResponse
import com.smartpacking.shared.model.WeatherInfo
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Maps between internal AI Worker models and shared DTOs.
 *
 * This separation allows:
 * - AI Worker to have strict internal validation
 * - Shared DTOs to remain flexible for cross-service communication
 * - Clear conversion layer for API responses
 */
@Component
class PackingResponseMapper {

    /**
     * Converts internal AiPackingResponse to shared PackingResponse DTO.
     *
     * @param aiResponse Internal AI service response
     * @param destination Travel destination
     * @param weatherInfo Optional weather information
     * @param cultureTips Optional culture tips
     * @return Shared DTO for API response
     */
    fun toSharedDto(
        aiResponse: AiPackingResponse,
        destination: String,
        weatherInfo: WeatherInfo? = null,
        cultureTips: List<String> = emptyList()
    ): PackingResponse {
        return PackingResponse(
            id = UUID.randomUUID(),
            destination = destination,
            categories = SharedPackingCategories(
                clothing = aiResponse.categories.clothing.map { it.toSharedDto() },
                tech = aiResponse.categories.tech.map { it.toSharedDto() },
                hygiene = aiResponse.categories.hygiene.map { it.toSharedDto() },
                documents = aiResponse.categories.documents.map { it.toSharedDto() },
                other = aiResponse.categories.other.map { it.toSharedDto() }
            ),
            weatherInfo = weatherInfo,
            cultureTips = cultureTips,
            specialNotes = null
        )
    }

    /**
     * Converts internal PackingItem to shared PackingItem DTO.
     */
    private fun PackingItem.toSharedDto(): SharedPackingItem {
        return SharedPackingItem(
            item = this.item,
            quantity = this.quantity,
            reason = this.reason
        )
    }
}
