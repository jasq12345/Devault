package dev.devault.ingestion.service

import dev.devault.ingestion.client.github.GithubClient
import dev.devault.ingestion.dto.response.RateLimitResponseDto
import dev.devault.ingestion.dto.response.toResponse
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Reads the GitHub rate limit of a credential. It lives in its own class, because
 * GithubClient already depends on CredentialService and the reverse would be a cycle.
 */
@Service
class RateLimitService(
    private val credentialService: CredentialService,
    private val githubClient: GithubClient
) {
    fun findForCredential(id: UUID, userId: UUID): RateLimitResponseDto {
        credentialService.requireOwned(id, userId)

        return githubClient.fetchRateLimit(userId, id).toResponse()
    }
}
