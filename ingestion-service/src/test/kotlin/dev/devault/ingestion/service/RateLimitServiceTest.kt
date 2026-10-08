package dev.devault.ingestion.service

import dev.devault.ingestion.client.github.GithubClient
import dev.devault.ingestion.client.github.dto.RateLimitInfo
import dev.devault.ingestion.exception.CredentialNotFoundException
import dev.devault.ingestion.exception.GithubApiException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class RateLimitServiceTest {
    private val credentialService = mockk<CredentialService>()
    private val githubClient = mockk<GithubClient>()
    private val service = RateLimitService(credentialService, githubClient)

    private val userId = UUID.randomUUID()
    private val credentialId = UUID.randomUUID()
    private val resetAt = Instant.parse("2026-01-01T00:45:00Z")

    @Test
    fun `returns the limit GitHub reports for the caller's credential`() {
        every { credentialService.requireOwned(credentialId, userId) } returns Unit
        every { githubClient.fetchRateLimit(userId, credentialId) } returns RateLimitInfo(remaining = 4321, resetAt = resetAt, cost = 1)

        val result = service.findForCredential(credentialId, userId)

        assertEquals(4321, result.remaining)
        assertEquals(resetAt, result.resetAt)
        verifyOrder {
            credentialService.requireOwned(credentialId, userId)
            githubClient.fetchRateLimit(userId, credentialId)
        }
    }

    @Test
    fun `answers not found for a missing or foreign credential without calling GitHub`() {
        every { credentialService.requireOwned(credentialId, userId) } throws
            CredentialNotFoundException("Credential not found")

        assertThrows<CredentialNotFoundException> {
            service.findForCredential(credentialId, userId)
        }

        verify(exactly = 0) { githubClient.fetchRateLimit(any(), any()) }
    }

    @Test
    fun `passes a GitHub failure on to the caller`() {
        every { credentialService.requireOwned(credentialId, userId) } returns Unit
        every { githubClient.fetchRateLimit(userId, credentialId) } throws GithubApiException("API rate limit exceeded")

        val exception = assertThrows<GithubApiException> {
            service.findForCredential(credentialId, userId)
        }

        assertEquals("API rate limit exceeded", exception.message)
    }
}
