package dev.devault.ingestion.service

import dev.devault.ingestion.dto.request.CredentialRequestDto
import dev.devault.ingestion.model.Credential
import dev.devault.ingestion.repository.CredentialRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CredentialServiceTest {
    private val repository = mockk<CredentialRepository>()
    private val service = CredentialService(repository)

    private val userId = UUID.randomUUID()
    private val credentialId = UUID.randomUUID()
    private val token = "test-token-not-a-real-pat"

    @Nested
    inner class Save {
        private val dto = CredentialRequestDto(label = "my label", token = token)
        private val saved = slot<Credential>()

        @Test
        fun `stores token and label for the calling user`() {
            every { repository.save(capture(saved)) } answers { saved.captured.apply { id = credentialId } }

            val result = service.save(dto, userId)

            assertEquals(credentialId, result.id)
            assertEquals(dto.label, result.label)
            assertEquals(token, saved.captured.token)
            assertEquals(dto.label, saved.captured.label)
            assertEquals(userId, saved.captured.connectedByUserId)
        }

        @Test
        fun `response does not expose the token`() {
            every { repository.save(capture(saved)) } answers { saved.captured.apply { id = credentialId } }

            val result = service.save(dto, userId)

            assertFalse(result.toString().contains(token))
        }
    }

    @Nested
    inner class FindAllForUser {
        @Test
        fun `returns credentials of the user without tokens`() {
            val first = Credential(UUID.randomUUID(), "first", userId, token)
            val second = Credential(UUID.randomUUID(), "second", userId, token)

            every { repository.findAllByConnectedByUserId(userId) } returns listOf(first, second)

            val result = service.findAllForUser(userId)

            assertEquals(listOf(first.id, second.id), result.map { it.id })
            assertEquals(listOf("first", "second"), result.map { it.label })
            assertFalse(result.toString().contains(token))
        }

        @Test
        fun `returns empty list when the user has no credentials`() {
            every { repository.findAllByConnectedByUserId(userId) } returns emptyList()

            assertEquals(emptyList(), service.findAllForUser(userId))
        }
    }

    @Nested
    inner class GetTokenForUser {
        @Test
        fun `returns token when the credential belongs to the user`() {
            val credential = Credential(credentialId, "label", userId, token)

            every { repository.findById(credentialId) } returns Optional.of(credential)

            assertEquals(token, service.getTokenForUser(credentialId, userId))
        }

        @Test
        fun `throws when the credential belongs to another user`() {
            val credential = Credential(credentialId, "label", UUID.randomUUID(), token)

            every { repository.findById(credentialId) } returns Optional.of(credential)

            assertThrows<AccessDeniedException> {
                service.getTokenForUser(credentialId, userId)
            }
        }

        @Test
        fun `throws when the credential does not exist`() {
            every { repository.findById(credentialId) } returns Optional.empty()

            assertThrows<NoSuchElementException> {
                service.getTokenForUser(credentialId, userId)
            }
        }
    }
}
