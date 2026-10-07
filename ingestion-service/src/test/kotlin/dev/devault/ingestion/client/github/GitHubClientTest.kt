package dev.devault.ingestion.client.github

import dev.devault.ingestion.exception.GitHubApiException
import dev.devault.ingestion.service.CredentialService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * No real GitHub call is made: MockRestServiceServer replaces the HTTP layer of the RestClient,
 * so the real message converters (JSON <-> DTO) are still exercised.
 */
class GitHubClientTest {
    private val credentialService = mockk<CredentialService>()

    // Same defaults as RestClient.create("https://api.github.com") in IngestionAutoConfiguration.
    private val restClientBuilder = RestClient.builder().baseUrl("https://api.github.com")
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()

    // The clock stands still and waiting is only recorded, so the rate limit tests run instantly.
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val pauses = mutableListOf<Duration>()
    private val client = object : GitHubClient(restClientBuilder.build(), credentialService, clock) {
        override fun pause(duration: Duration) {
            pauses += duration
        }
    }

    private val userId = UUID.randomUUID()
    private val credentialRef = UUID.randomUUID()
    private val token = "test-token-not-a-real-pat"

    @BeforeEach
    fun stubCredential() {
        every { credentialService.getTokenForUser(credentialRef, userId) } returns token
    }

    @Nested
    inner class FetchCommitHistory {
        @Test
        fun `posts the commit query with token and variables`() {
            server.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer $token"))
                .andExpect(jsonPath("$.query", containsString("history(first: 50")))
                .andExpect(jsonPath("$.variables.owner").value("octo"))
                .andExpect(jsonPath("$.variables.name").value("repo"))
                .andExpect(jsonPath("$.variables.cursor").value("cursor-1"))
                .andRespond(withSuccess(COMMIT_HISTORY_RESPONSE, MediaType.APPLICATION_JSON))

            client.fetchCommitHistory(userId, "octo", "repo", credentialRef, "cursor-1")

            server.verify()
        }

        @Test
        fun `sends no cursor for the first page`() {
            server.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(jsonPath("$.variables.cursor").doesNotExist())
                .andRespond(withSuccess(COMMIT_HISTORY_RESPONSE, MediaType.APPLICATION_JSON))

            client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)

            server.verify()
        }

        @Test
        fun `parses page info and commits`() {
            respondWith(COMMIT_HISTORY_RESPONSE)

            val page = client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)

            assertTrue(page.pageInfo.hasNextPage)
            assertEquals("cursor-1", page.pageInfo.endCursor)
            assertEquals(2, page.nodes.size)

            val first = page.nodes.first()
            assertEquals("abc123", first.oid)
            assertEquals("feat: first commit", first.message)
            assertEquals(Instant.parse("2025-12-31T10:15:30Z"), first.committedDate)
            assertEquals("Jan", first.author?.name)
            assertEquals("jan@example.com", first.author?.email)

            assertNull(page.nodes.last().author)
        }

        @Test
        fun `remembers the rate limit of the last response`() {
            respondWith(COMMIT_HISTORY_RESPONSE)
            assertNull(client.getLastKnownRateLimit(credentialRef))

            client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)

            val rateLimit = client.getLastKnownRateLimit(credentialRef)
            assertEquals(4990, rateLimit?.remaining)
            assertEquals(1, rateLimit?.cost)
            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), rateLimit?.resetAt)
        }

        @Test
        fun `throws when the repository has no default branch`() {
            respondWith(EMPTY_REPOSITORY_RESPONSE)

            val exception = assertThrows<GitHubApiException> {
                client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)
            }

            assertEquals("No commit history found", exception.message)
        }
    }

    @Nested
    inner class FetchPullRequests {
        @Test
        fun `posts the pull request query with token and variables`() {
            server.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer $token"))
                .andExpect(jsonPath("$.query", containsString("pullRequests(first: 50")))
                .andExpect(jsonPath("$.variables.owner").value("octo"))
                .andExpect(jsonPath("$.variables.name").value("repo"))
                .andExpect(jsonPath("$.variables.cursor").value("cursor-1"))
                .andRespond(withSuccess(PULL_REQUESTS_RESPONSE, MediaType.APPLICATION_JSON))

            client.fetchPullRequests(userId, "octo", "repo", credentialRef, "cursor-1")

            server.verify()
        }

        @Test
        fun `parses pull requests including a missing body and author`() {
            respondWith(PULL_REQUESTS_RESPONSE)

            val page = client.fetchPullRequests(userId, "octo", "repo", credentialRef, null)

            assertFalse(page.pageInfo.hasNextPage)
            assertNull(page.pageInfo.endCursor)

            val first = page.nodes.first()
            assertEquals(12, first.number)
            assertEquals("Add backfill", first.title)
            assertEquals("Closes #7", first.body)
            assertEquals("https://github.com/octo/repo/pull/12", first.url)
            assertEquals("MERGED", first.state)
            assertEquals(Instant.parse("2025-12-30T08:00:00Z"), first.createdAt)
            assertEquals("jan", first.author?.login)

            val second = page.nodes.last()
            assertNull(second.body)
            assertNull(second.author)
            assertEquals(4980, client.getLastKnownRateLimit(credentialRef)?.remaining)
        }
    }

    @Nested
    inner class FetchIssues {
        @Test
        fun `posts the issues query with token and variables`() {
            server.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer $token"))
                .andExpect(jsonPath("$.query", containsString("issues(first: 50")))
                .andExpect(jsonPath("$.variables.owner").value("octo"))
                .andExpect(jsonPath("$.variables.name").value("repo"))
                .andExpect(jsonPath("$.variables.cursor").value("cursor-1"))
                .andRespond(withSuccess(ISSUES_RESPONSE, MediaType.APPLICATION_JSON))

            client.fetchIssues(userId, "octo", "repo", credentialRef, "cursor-1")

            server.verify()
        }

        @Test
        fun `parses issues`() {
            respondWith(ISSUES_RESPONSE)

            val page = client.fetchIssues(userId, "octo", "repo", credentialRef, null)

            assertTrue(page.pageInfo.hasNextPage)
            assertEquals("issue-cursor", page.pageInfo.endCursor)

            val issue = page.nodes.single()
            assertEquals(7, issue.number)
            assertEquals("Backfill is slow", issue.title)
            assertEquals("Takes minutes", issue.body)
            assertEquals("OPEN", issue.state)
            assertEquals("jan", issue.author?.login)
            assertEquals(4970, client.getLastKnownRateLimit(credentialRef)?.remaining)
        }
    }

    @Nested
    inner class Errors {
        @Test
        fun `GraphQL errors with HTTP 200 become GitHubApiException`() {
            respondWith("""{ "data": null, "errors": [ { "message": "Something went wrong" } ] }""")

            val exception = assertThrows<GitHubApiException> {
                client.fetchIssues(userId, "octo", "repo", credentialRef, null)
            }

            assertEquals("Something went wrong", exception.message)
        }

        @Test
        fun `error entries with extra GitHub fields are still read`() {
            respondWith(
                """
                {
                  "data": null,
                  "errors": [
                    {
                      "type": "FORBIDDEN",
                      "path": ["repository"],
                      "locations": [ { "line": 7, "column": 3 } ],
                      "extensions": { "saml_failure": false },
                      "message": "Resource not accessible by personal access token"
                    }
                  ]
                }
                """
            )

            val exception = assertThrows<GitHubApiException> {
                client.fetchPullRequests(userId, "octo", "repo", credentialRef, null)
            }

            assertEquals("Resource not accessible by personal access token", exception.message)
        }

        @Test
        fun `unknown repository becomes GitHubApiException with the GitHub message`() {
            respondWith(REPOSITORY_NOT_FOUND_RESPONSE)

            val exception = assertThrows<GitHubApiException> {
                client.fetchCommitHistory(userId, "octo", "missing", credentialRef, null)
            }

            assertEquals("Could not resolve to a Repository with the name 'octo/missing'.", exception.message)
        }

        @Test
        fun `unknown repository is reported the same way for pull requests and issues`() {
            repeat(2) { respondWith(REPOSITORY_NOT_FOUND_RESPONSE) }

            val pullRequestsException = assertThrows<GitHubApiException> {
                client.fetchPullRequests(userId, "octo", "missing", credentialRef, null)
            }
            val issuesException = assertThrows<GitHubApiException> {
                client.fetchIssues(userId, "octo", "missing", credentialRef, null)
            }

            assertEquals("Could not resolve to a Repository with the name 'octo/missing'.", pullRequestsException.message)
            assertEquals("Could not resolve to a Repository with the name 'octo/missing'.", issuesException.message)
        }

        @Test
        fun `null repository without errors becomes GitHubApiException`() {
            repeat(3) { respondWith(NULL_REPOSITORY_RESPONSE) }

            assertThrows<GitHubApiException> {
                client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)
            }
            assertThrows<GitHubApiException> {
                client.fetchPullRequests(userId, "octo", "repo", credentialRef, null)
            }
            assertThrows<GitHubApiException> {
                client.fetchIssues(userId, "octo", "repo", credentialRef, null)
            }
        }

        @Test
        fun `response without a body becomes GitHubApiException`() {
            server.expect(requestTo("https://api.github.com/graphql")).andRespond(withSuccess())

            val exception = assertThrows<GitHubApiException> {
                client.fetchIssues(userId, "octo", "repo", credentialRef, null)
            }

            assertEquals("Empty response from GitHub API", exception.message)
        }

        @Test
        fun `HTTP 401 for a bad token surfaces as RestClientResponseException`() {
            server.expect(requestTo("https://api.github.com/graphql"))
                .andRespond(
                    withUnauthorizedRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""{ "message": "Bad credentials" }""")
                )

            val exception = assertThrows<RestClientResponseException> {
                client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)
            }

            assertEquals(401, exception.statusCode.value())
            assertFalse(exception.message.orEmpty().contains(token))
        }
    }

    @Nested
    inner class Credentials {
        @Test
        fun `reads the token through the ownership check`() {
            respondWith(ISSUES_RESPONSE)

            client.fetchIssues(userId, "octo", "repo", credentialRef, null)

            verify(exactly = 1) { credentialService.getTokenForUser(credentialRef, userId) }
        }

        @Test
        fun `does not call GitHub when the credential belongs to another user`() {
            every { credentialService.getTokenForUser(credentialRef, userId) } throws AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> {
                client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)
            }
            assertThrows<AccessDeniedException> {
                client.fetchPullRequests(userId, "octo", "repo", credentialRef, null)
            }
            assertThrows<AccessDeniedException> {
                client.fetchIssues(userId, "octo", "repo", credentialRef, null)
            }

            // No expectation was registered, so any request would have failed the test already.
            server.verify()
        }
    }

    @Nested
    inner class RateLimitGate {
        private val otherCredentialRef = UUID.randomUUID()

        @BeforeEach
        fun stubOtherCredential() {
            every { credentialService.getTokenForUser(otherCredentialRef, userId) } returns "other-test-token-not-a-real-pat"
        }

        // MockRestServiceServer wants every response registered before the first request,
        // so each test first queues the limits GitHub will report and then sends the requests.
        private fun queueLimit(remaining: Int, resetAt: String) = respondWith(issuesResponse(remaining, resetAt))

        private fun fetchIssues(credential: UUID = credentialRef) =
            client.fetchIssues(userId, "octo", "repo", credential, null)

        @Test
        fun `keeps the limits of two credentials apart`() {
            queueLimit(remaining = 4000, resetAt = "2026-01-01T01:00:00Z")
            queueLimit(remaining = 12, resetAt = "2026-01-01T00:30:00Z")

            fetchIssues()
            fetchIssues(otherCredentialRef)

            assertEquals(4000, client.getLastKnownRateLimit(credentialRef)?.remaining)
            assertEquals(12, client.getLastKnownRateLimit(otherCredentialRef)?.remaining)
        }

        @Test
        fun `does not wait before the first request of a credential`() {
            queueLimit(remaining = 4000, resetAt = "2026-01-01T01:00:00Z")

            fetchIssues()

            assertEquals(emptyList(), pauses)
        }

        @Test
        fun `does not wait while more points than the reserve are left`() {
            queueLimit(remaining = 51, resetAt = "2026-01-01T00:10:00Z")
            queueLimit(remaining = 50, resetAt = "2026-01-01T00:10:00Z")

            fetchIssues()
            fetchIssues()

            assertEquals(emptyList(), pauses)
        }

        @Test
        fun `waits until the reset plus a margin once the points are down to the reserve`() {
            queueLimit(remaining = 50, resetAt = "2026-01-01T00:10:00Z")
            queueLimit(remaining = 4999, resetAt = "2026-01-01T01:10:00Z")

            fetchIssues()
            fetchIssues()

            // The clock shows 00:00:00, the limit resets at 00:10:00, the margin is 2 seconds.
            assertEquals(listOf(Duration.ofMinutes(10).plusSeconds(2)), pauses)
            server.verify()
        }

        @Test
        fun `goes back to full speed after the response with the renewed limit`() {
            queueLimit(remaining = 50, resetAt = "2026-01-01T00:10:00Z")
            queueLimit(remaining = 4999, resetAt = "2026-01-01T01:10:00Z")
            queueLimit(remaining = 4998, resetAt = "2026-01-01T01:10:00Z")

            repeat(3) { fetchIssues() }

            // Only the second request waited; the third one saw the renewed limit.
            assertEquals(1, pauses.size)
        }

        @Test
        fun `does not wait when the reset time has already passed`() {
            queueLimit(remaining = 3, resetAt = "2025-12-31T23:00:00Z")
            queueLimit(remaining = 4999, resetAt = "2026-01-01T01:00:00Z")

            fetchIssues()
            fetchIssues()

            assertEquals(emptyList(), pauses)
        }

        @Test
        fun `a used up credential does not hold back another one`() {
            queueLimit(remaining = 0, resetAt = "2026-01-01T00:10:00Z")
            queueLimit(remaining = 4000, resetAt = "2026-01-01T01:00:00Z")

            fetchIssues()
            fetchIssues(otherCredentialRef)

            assertEquals(emptyList(), pauses)
        }

        @Test
        fun `applies to commits and pull requests as well`() {
            queueLimit(remaining = 10, resetAt = "2026-01-01T00:01:00Z")
            respondWith(COMMIT_HISTORY_RESPONSE)
            queueLimit(remaining = 10, resetAt = "2026-01-01T00:01:00Z")
            respondWith(PULL_REQUESTS_RESPONSE)

            fetchIssues()
            client.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)
            fetchIssues()
            client.fetchPullRequests(userId, "octo", "repo", credentialRef, null)

            // The commit request and the pull request one each followed a response with 10 points left.
            assertEquals(List(2) { Duration.ofSeconds(62) }, pauses)
        }

        @Test
        fun `rejects a foreign credential at once instead of waiting first`() {
            queueLimit(remaining = 0, resetAt = "2026-01-01T00:10:00Z")
            fetchIssues()
            every { credentialService.getTokenForUser(credentialRef, userId) } throws AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> { fetchIssues() }

            assertEquals(emptyList(), pauses)
        }
    }

    private fun respondWith(json: String) {
        server.expect(requestTo("https://api.github.com/graphql"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
    }

    private fun issuesResponse(remaining: Int, resetAt: String) = """
        {
          "data": {
            "rateLimit": { "remaining": $remaining, "resetAt": "$resetAt", "cost": 1 },
            "repository": { "issues": { "pageInfo": { "hasNextPage": false, "endCursor": null }, "nodes": [] } }
          }
        }
    """

    private companion object {
        const val COMMIT_HISTORY_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4990, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": {
                  "defaultBranchRef": {
                    "target": {
                      "history": {
                        "pageInfo": { "hasNextPage": true, "endCursor": "cursor-1" },
                        "nodes": [
                          {
                            "oid": "abc123",
                            "message": "feat: first commit",
                            "committedDate": "2025-12-31T10:15:30Z",
                            "author": { "name": "Jan", "email": "jan@example.com" }
                          },
                          {
                            "oid": "def456",
                            "message": "fix: second commit",
                            "committedDate": "2025-12-31T11:15:30Z",
                            "author": null
                          }
                        ]
                      }
                    }
                  }
                }
              }
            }
        """

        const val EMPTY_REPOSITORY_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4990, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": { "defaultBranchRef": null }
              }
            }
        """

        const val PULL_REQUESTS_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4980, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": {
                  "pullRequests": {
                    "pageInfo": { "hasNextPage": false, "endCursor": null },
                    "nodes": [
                      {
                        "number": 12,
                        "title": "Add backfill",
                        "body": "Closes #7",
                        "url": "https://github.com/octo/repo/pull/12",
                        "state": "MERGED",
                        "createdAt": "2025-12-30T08:00:00Z",
                        "author": { "login": "jan" }
                      },
                      {
                        "number": 13,
                        "title": "Bump dependency",
                        "body": null,
                        "url": "https://github.com/octo/repo/pull/13",
                        "state": "OPEN",
                        "createdAt": "2025-12-30T09:00:00Z",
                        "author": null
                      }
                    ]
                  }
                }
              }
            }
        """

        const val ISSUES_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4970, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": {
                  "issues": {
                    "pageInfo": { "hasNextPage": true, "endCursor": "issue-cursor" },
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

        const val NULL_REPOSITORY_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4990, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": null
              }
            }
        """

        // Shape GitHub uses for a repository that does not exist or is not visible to the token:
        // HTTP 200, the failed field is null and the reason is in "errors".
        const val REPOSITORY_NOT_FOUND_RESPONSE = """
            {
              "data": {
                "rateLimit": { "remaining": 4990, "resetAt": "2026-01-01T00:00:00Z", "cost": 1 },
                "repository": null
              },
              "errors": [
                {
                  "type": "NOT_FOUND",
                  "path": ["repository"],
                  "locations": [ { "line": 7, "column": 3 } ],
                  "message": "Could not resolve to a Repository with the name 'octo/missing'."
                }
              ]
            }
        """
    }
}
