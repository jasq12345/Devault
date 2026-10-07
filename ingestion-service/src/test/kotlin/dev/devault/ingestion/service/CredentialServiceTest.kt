package dev.devault.ingestion.service

import dev.devault.ingestion.dto.request.CredentialRequestDto
import dev.devault.ingestion.exception.CredentialNotFoundException
import dev.devault.ingestion.model.Credential
import dev.devault.ingestion.repository.CredentialRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

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

    @Nested
    inner class RequireOwned {
        @Test
        fun `passes when the credential belongs to the user`() {
            every { repository.findByIdAndConnectedByUserId(credentialId, userId) } returns
                Credential(credentialId, "label", userId, token)

            service.requireOwned(credentialId, userId)
        }

        @Test
        fun `throws not found when the credential is missing or belongs to another user`() {
            every { repository.findByIdAndConnectedByUserId(credentialId, userId) } returns null

            val exception = assertThrows<CredentialNotFoundException> {
                service.requireOwned(credentialId, userId)
            }

            // GlobalExceptionHandler maps NoSuchElementException to 404.
            assertIs<NoSuchElementException>(exception)
        }
    }

    @Nested
    inner class Delete {
        private val credential = Credential(credentialId, "label", userId, token)

        @Test
        fun `deletes a credential of the caller`() {
            every { repository.findByIdAndConnectedByUserId(credentialId, userId) } returns credential
            every { repository.delete(credential) } returns Unit

            service.delete(credentialId, userId)

            verify(exactly = 1) { repository.delete(credential) }
        }

        @Test
        fun `throws not found and deletes nothing when the credential is missing or belongs to another user`() {
            every { repository.findByIdAndConnectedByUserId(credentialId, userId) } returns null

            assertThrows<CredentialNotFoundException> {
                service.delete(credentialId, userId)
            }

            verify(exactly = 0) { repository.delete(any()) }
        }
    }
}
