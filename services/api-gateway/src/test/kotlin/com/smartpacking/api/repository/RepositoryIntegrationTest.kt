package com.smartpacking.api.repository

import com.smartpacking.api.entity.ChatMessage
import com.smartpacking.api.entity.PackingList
import com.smartpacking.api.entity.Session
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.*

/**
 * Integration test for repositories against actual PostgreSQL database.
 *
 * To run this test:
 * - Make sure PostgreSQL is running on localhost:5432
 * - Database 'packing_assistant' should exist with schema already applied
 * - Use @ActiveProfiles("integration") to run against real database
 *
 * Note: For regular builds, this test is disabled (uses in-memory H2 with test profile)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
class RepositoryIntegrationTest {

    @Autowired
    private lateinit var sessionRepository: SessionRepository

    @Autowired
    private lateinit var packingListRepository: PackingListRepository

    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository

    @Test
    fun `should create and find session`() {
        // Given
        val sessionToken = "test-session-${UUID.randomUUID()}"
        val session = Session(sessionToken = sessionToken)

        // When
        val savedSession = sessionRepository.save(session)

        // Then
        assertNotNull(savedSession.id)
        assertEquals(sessionToken, savedSession.sessionToken)
        assertTrue(savedSession.isActive)

        // Verify we can find it
        val foundSession = sessionRepository.findBySessionToken(sessionToken)
        assertNotNull(foundSession)
        assertEquals(savedSession.id, foundSession?.id)
    }

    @Test
    fun `should create packing list with session`() {
        // Given - Create a session first
        val session = sessionRepository.save(Session(sessionToken = "test-${UUID.randomUUID()}"))

        // When - Create packing list
        val packingList = PackingList(
            session = session,
            destination = "Tokyo",
            durationDays = 7,
            travelType = "VACATION",
            travelDate = LocalDate.of(2025, 5, 1),
            season = "SPRING",
            itemsJson = """{"categories": {"clothing": []}}""",
            weatherInfo = """{"temperature_range": {"min": 12, "max": 20}}""",
            cultureTips = arrayOf("Tip 1", "Tip 2")
        )
        val savedPackingList = packingListRepository.save(packingList)

        // Then
        assertNotNull(savedPackingList.id)
        assertEquals("Tokyo", savedPackingList.destination)
        assertEquals(7, savedPackingList.durationDays)
        assertEquals("VACATION", savedPackingList.travelType)
        assertEquals("SPRING", savedPackingList.season)

        // Verify we can find it by session
        val foundLists = packingListRepository.findBySessionId(session.id!!)
        assertEquals(1, foundLists.size)
        assertEquals(savedPackingList.id, foundLists[0].id)
    }

    @Test
    fun `should create chat message for packing list`() {
        // Given - Create session and packing list
        val session = sessionRepository.save(Session(sessionToken = "test-${UUID.randomUUID()}"))
        val packingList = packingListRepository.save(
            PackingList(
                session = session,
                destination = "Dubai",
                durationDays = 3,
                travelType = "BUSINESS",
                season = "SUMMER",
                itemsJson = """{"categories": {}}"""
            )
        )

        // When - Create chat message
        val chatMessage = ChatMessage(
            packingList = packingList,
            role = "USER",
            content = "Do I need an adapter?",
            aiModel = "gpt-4",
            tokensUsed = 15
        )
        val savedMessage = chatMessageRepository.save(chatMessage)

        // Then
        assertNotNull(savedMessage.id)
        assertEquals("USER", savedMessage.role)
        assertEquals("Do I need an adapter?", savedMessage.content)

        // Verify we can find it by packing list
        val foundMessages = chatMessageRepository.findByPackingListIdOrderByCreatedAtAsc(packingList.id!!)
        assertEquals(1, foundMessages.size)
        assertEquals(savedMessage.id, foundMessages[0].id)
    }

    @Test
    fun `should handle cascade delete for session`() {
        // Given - Create session with packing list
        val session = sessionRepository.save(Session(sessionToken = "test-${UUID.randomUUID()}"))
        val packingList = packingListRepository.save(
            PackingList(
                session = session,
                destination = "Iceland",
                durationDays = 5,
                travelType = "BACKPACKING",
                season = "WINTER",
                itemsJson = """{"categories": {}}"""
            )
        )
        val packingListId = packingList.id!!

        // When - Delete session
        sessionRepository.delete(session)
        sessionRepository.flush()

        // Then - Packing list should also be deleted (cascade)
        val foundPackingList = packingListRepository.findById(packingListId)
        assertFalse(foundPackingList.isPresent)
    }

    @Test
    fun `should find packing lists by destination`() {
        // Given - Create multiple packing lists
        val session = sessionRepository.save(Session(sessionToken = "test-${UUID.randomUUID()}"))

        packingListRepository.save(
            PackingList(
                session = session,
                destination = "Tokyo",
                durationDays = 7,
                travelType = "VACATION",
                season = "SPRING",
                itemsJson = """{"categories": {}}"""
            )
        )

        packingListRepository.save(
            PackingList(
                session = session,
                destination = "Dubai",
                durationDays = 3,
                travelType = "BUSINESS",
                season = "WINTER",
                itemsJson = """{"categories": {}}"""
            )
        )

        // When - Search by destination
        val tokyoLists = packingListRepository.findByDestinationContainingIgnoreCase("tokyo")
        val dubaiLists = packingListRepository.findByDestinationContainingIgnoreCase("Dubai")

        // Then
        assertTrue(tokyoLists.isNotEmpty())
        assertTrue(dubaiLists.isNotEmpty())
        assertTrue(tokyoLists.any { it.destination == "Tokyo" })
        assertTrue(dubaiLists.any { it.destination == "Dubai" })
    }
}
