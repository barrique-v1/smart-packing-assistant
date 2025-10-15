package com.smartpacking.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.smartpacking.api.entity.PackingList
import com.smartpacking.api.exception.PackingListNotFoundException
import com.smartpacking.api.repository.PackingListRepository
import com.smartpacking.shared.dto.PackingCategories
import com.smartpacking.shared.dto.PackingItem
import com.smartpacking.shared.dto.PackingRequest
import com.smartpacking.shared.dto.PackingResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for managing packing lists
 */
@Service
@Transactional
class PackingListService(
    private val packingListRepository: PackingListRepository,
    private val sessionService: SessionService,
    private val aiWorkerClient: AiWorkerClient
) {

    private val logger = LoggerFactory.getLogger(PackingListService::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    /**
     * Generate a new packing list using AI Worker
     */
    fun generatePackingList(request: PackingRequest, sessionToken: String): PackingResponse {
        logger.info("Generating packing list for ${request.destination}")

        val startTime = System.currentTimeMillis()

        // Validate and get session
        val session = sessionService.findByToken(sessionToken)

        // Update session activity
        sessionService.updateActivity(sessionToken)

        // Call AI Worker to generate packing list
        val aiResponse = aiWorkerClient.generatePackingList(request)

        val generationTime = (System.currentTimeMillis() - startTime).toInt()

        // Convert categories to JSON
        val categoriesJson = objectMapper.writeValueAsString(aiResponse.categories)

        // Convert weather info to JSON
        val weatherJson = aiResponse.weatherInfo?.let { objectMapper.writeValueAsString(it) }

        // Save to database
        val packingList = PackingList(
            session = session,
            destination = request.destination,
            durationDays = request.durationDays,
            travelType = request.travelType.name,
            travelDate = request.travelDate,
            season = request.season.name,
            itemsJson = categoriesJson,
            weatherInfo = weatherJson,
            cultureTips = aiResponse.cultureTips,
            aiModel = "mock-gpt-4",
            generationTimeMs = generationTime
        )

        val savedList = packingListRepository.save(packingList)
        logger.info("Saved packing list with id: ${savedList.id}")

        // Return response with actual ID
        return PackingResponse(
            id = savedList.id!!,
            destination = aiResponse.destination,
            categories = aiResponse.categories,
            weatherInfo = aiResponse.weatherInfo,
            cultureTips = aiResponse.cultureTips
        )
    }

    /**
     * Get a packing list by ID
     */
    @Transactional(readOnly = true)
    fun getPackingList(id: UUID): PackingResponse {
        val packingList = packingListRepository.findById(id)
            .orElseThrow { PackingListNotFoundException(id) }

        return convertToResponse(packingList)
    }

    /**
     * Get all packing lists for a session
     */
    @Transactional(readOnly = true)
    fun getPackingListsBySession(sessionToken: String): List<PackingResponse> {
        val session = sessionService.findByToken(sessionToken)
        val packingLists = packingListRepository.findBySessionId(session.id!!)

        return packingLists.map { convertToResponse(it) }
    }

    /**
     * Get recent packing lists for a session
     */
    @Transactional(readOnly = true)
    fun getRecentPackingLists(sessionToken: String, limit: Int = 10): List<PackingResponse> {
        val session = sessionService.findByToken(sessionToken)
        val packingLists = packingListRepository.findRecentBySession(session.id!!)

        return packingLists.take(limit).map { convertToResponse(it) }
    }

    /**
     * Search packing lists by destination
     */
    @Transactional(readOnly = true)
    fun searchByDestination(destination: String): List<PackingResponse> {
        val packingLists = packingListRepository.findByDestinationContainingIgnoreCase(destination)
        return packingLists.map { convertToResponse(it) }
    }

    /**
     * Convert PackingList entity to PackingResponse DTO
     */
    private fun convertToResponse(packingList: PackingList): PackingResponse {
        // Parse categories JSON
        val categories = objectMapper.readValue(packingList.itemsJson, PackingCategories::class.java)

        // Parse weather info if present
        val weatherInfo = packingList.weatherInfo?.let {
            objectMapper.readValue(it, com.smartpacking.shared.model.WeatherInfo::class.java)
        }

        return PackingResponse(
            id = packingList.id!!,
            destination = packingList.destination,
            categories = categories,
            weatherInfo = weatherInfo,
            cultureTips = packingList.cultureTips ?: emptyList()
        )
    }
}
