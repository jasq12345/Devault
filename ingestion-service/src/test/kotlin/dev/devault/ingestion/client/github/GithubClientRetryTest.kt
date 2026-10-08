package dev.devault.ingestion.client.github

import dev.devault.ingestion.config.IngestionAutoConfiguration
import dev.devault.ingestion.service.CredentialService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.io.IOException
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Retrying is done by a Spring proxy around GithubClient, so this test needs a (small) application context:
 * the real IngestionAutoConfiguration plus a GithubClient whose HTTP layer is MockRestServiceServer.
 * No database and no real GitHub call. The first retry delay is 1 ms here instead of one second.
 */
@SpringJUnitConfig(GithubClientRetryTest.TestConfig::class)
@TestPropertySource(properties = ["ingestion.github.retry.delay=1"])
class GithubClientRetryTest(
    @Autowired private val client: GithubClient,
    @Autowired private val server: MockRestServiceServer,
    @Autowired private val credentialService: CredentialService
) {
    private val userId = UUID.randomUUID()
    private val credentialRef = UUID.randomUUID()

    @BeforeEach
    fun resetMocks() {
        server.reset()
        clearMocks(credentialService)
        every { credentialService.getTokenForUser(credentialRef, userId) } returns "test-token-not-a-real-pat"
    }

    @Test
    fun `retries a 503 and returns the page once GitHub answers again`() {
        server.expect(ExpectedCount.times(2), requestTo(GRAPHQL_URL)).andRespond(withServiceUnavailable())
        server.expect(ExpectedCount.once(), requestTo(GRAPHQL_URL))
            .andRespond(withSuccess(ISSUES_RESPONSE, MediaType.APPLICATION_JSON))

        val page = client.fetchIssues(userId, "octo", "repo", credentialRef, null)

        assertEquals(listOf(7), page.nodes.map { it.number })
        server.verify()
    }

    @Test
    fun `retries a connection error`() {
        server.expect(ExpectedCount.once(), requestTo(GRAPHQL_URL)).andRespond(withException(IOException("connection reset")))
        server.expect(ExpectedCount.once(), requestTo(GRAPHQL_URL))
            .andRespond(withSuccess(ISSUES_RESPONSE, MediaType.APPLICATION_JSON))

        val page = client.fetchIssues(userId, "octo", "repo", credentialRef, null)

        assertEquals(listOf(7), page.nodes.map { it.number })
        server.verify()
    }

    @Test
    fun `gives up after five retries and rethrows the GitHub error`() {
        server.expect(ExpectedCount.times(6), requestTo(GRAPHQL_URL)).andRespond(withServiceUnavailable())

        val exception = assertThrows<HttpServerErrorException> {
            client.fetchIssues(userId, "octo", "repo", credentialRef, null)
        }

        assertEquals(503, exception.statusCode.value())
        server.verify()
    }

    @Test
    fun `does not retry a 401 for a bad token`() {
        server.expect(ExpectedCount.once(), requestTo(GRAPHQL_URL)).andRespond(withUnauthorizedRequest())

        assertThrows<HttpClientErrorException> {
            client.fetchIssues(userId, "octo", "repo", credentialRef, null)
        }

        server.verify()
    }

    @Test
    fun `does not retry when the credential belongs to another user`() {
        every { credentialService.getTokenForUser(credentialRef, userId) } throws AccessDeniedException("Access denied")

        assertThrows<AccessDeniedException> {
            client.fetchIssues(userId, "octo", "repo", credentialRef, null)
        }

        verify(exactly = 1) { credentialService.getTokenForUser(credentialRef, userId) }
        server.verify()
    }

    @Configuration
    @Import(IngestionAutoConfiguration::class)
    class TestConfig {
        private val restClientBuilder = RestClient.builder().baseUrl("https://api.github.com")

        @Bean
        fun mockServer(): MockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build()

        @Bean
        fun credentialService(): CredentialService = mockk()

        // Depends on mockServer so that the builder is already bound to it when the client is built.
        @Bean
        fun githubClient(mockServer: MockRestServiceServer, credentialService: CredentialService, clock: Clock) =
            GithubClient(restClientBuilder.build(), credentialService, clock)
    }

    private companion object {
        const val GRAPHQL_URL = "https://api.github.com/graphql"

        const val ISSUES_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4970, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": {
                  "issues": {
                    "pageInfo": { "hasNextPage": false, "endCursor": null },
                    "nodes": [
                      {
                        "number": 7,
                        "title": "Backfill is slow",
                        "body": "Takes minutes",
                        "url": "https://github.com/octo/repo/issues/7",
                        "state": "OPEN",
                        "createdAt": "2025-12-29T08:00:00Z",
                        "author": { "login": "jan" }
                      }
                    ]
                  }
                }
              }
            }
        """
    }
}
