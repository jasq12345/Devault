package dev.devault.ingestion.client.github

import dev.devault.ingestion.client.github.dto.GraphQLResponse
import dev.devault.ingestion.client.github.dto.HistoryConnection
import dev.devault.ingestion.client.github.dto.IssueLikeConnection
import dev.devault.ingestion.client.github.dto.IssuesResponse
import dev.devault.ingestion.client.github.dto.PullRequestsResponse
import dev.devault.ingestion.client.github.dto.RateLimitInfo
import dev.devault.ingestion.client.github.dto.RateLimitResponse
import dev.devault.ingestion.client.github.dto.RepositoryHistoryResponse
import dev.devault.ingestion.exception.GitHubApiException
import dev.devault.ingestion.service.CredentialService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Retries only what can fix itself: GitHub 5xx and connection errors. 4xx and GraphQL errors fail at once.
// Waits 1s, 2s, 4s, 8s, 16s between attempts; the first delay (ms) comes from ingestion.github.retry.delay.
@Component
@Retryable(
    includes = [HttpServerErrorException::class, ResourceAccessException::class],
    maxRetries = 5,
    delayString = "\${ingestion.github.retry.delay:1000}",
    multiplier = 2.0,
    maxDelay = 30_000
)
class GitHubClient(
    @Qualifier("gitHubRestClient") private val restClient: RestClient,
    private val credentialService: CredentialService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // GitHub counts the limit per token, so the last known value is kept per credential.
    private val lastKnownRateLimit: ConcurrentHashMap<UUID, RateLimitInfo> = ConcurrentHashMap()

    companion object{
        // Points left untouched, so that requests already on their way do not overrun the limit.
        private const val RATE_LIMIT_RESERVE = 50
        // Extra wait after resetAt, in case the clocks of GitHub and this machine differ.
        private val RESET_MARGIN: Duration = Duration.ofSeconds(2)

        private const val COMMIT_HISTORY_QUERY: String = """
            query(${'$'}owner: String!, ${'$'}name: String!, ${'$'}cursor: String) {
              rateLimit {
                remaining
                resetAt
                cost
              }
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                defaultBranchRef {
                  target {
                    ... on Commit {
                      history(first: 50, after: ${'$'}cursor) {
                        pageInfo { hasNextPage endCursor }
                        nodes { oid message committedDate author { name email } }
                      }
                    }
                  }
                }
              }
            }
        """

        private const val PULL_REQUEST_QUERY: String = """
            query(${'$'}owner: String!, ${'$'}name: String!, ${'$'}cursor: String) {
              rateLimit {
                remaining
                resetAt
                cost
              }
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                pullRequests(first: 50, after: ${'$'}cursor) {
                  pageInfo { hasNextPage endCursor }
                  nodes {
                    number
                    title
                    body
                    url
                    state
                    createdAt
                    author { login }
                  }
                }
              }
            }
        """

        private const val ISSUES_QUERY: String = """
            query(${'$'}owner: String!, ${'$'}name: String!, ${'$'}cursor: String) {
              rateLimit {
                remaining
                resetAt
                cost
              }
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                issues(first: 50, after: ${'$'}cursor) {
                  pageInfo { hasNextPage endCursor }
                  nodes {
                    number
                    title
                    body
                    url
                    state
                    createdAt
                    author { login }
                  }
                }
              }
            }
        """

        private const val RATE_LIMIT_QUERY: String = """
            query {
              rateLimit {
                remaining
                resetAt
                cost
              }
            }
        """
    }
    fun fetchCommitHistory(userId: UUID, owner: String, name: String, credentialRef: UUID, cursor: String?): HistoryConnection {
        val token = credentialService.getTokenForUser(credentialRef, userId)
        awaitRateLimit(credentialRef)
        val requestBody = mapOf(
            "query" to COMMIT_HISTORY_QUERY,
            "variables" to mapOf("owner" to owner, "name" to name, "cursor" to cursor)
        )

        val response = restClient.post()
            .uri("/graphql")
            .header("Authorization", "Bearer $token")
            .body(requestBody)
            .retrieve()
            .body(object : ParameterizedTypeReference<GraphQLResponse<RepositoryHistoryResponse>>() {})
            ?: throw GitHubApiException("Empty response from GitHub API")

        val unwrappedResponse = response.unwrap()

        lastKnownRateLimit[credentialRef] = unwrappedResponse.rateLimit

        return unwrappedResponse.repository?.defaultBranchRef?.target?.history
            ?: throw GitHubApiException("No commit history found")
    }

    fun fetchPullRequests(userId: UUID, owner: String, name: String, credentialRef: UUID, cursor: String?): IssueLikeConnection {
        val token = credentialService.getTokenForUser(credentialRef, userId)
        awaitRateLimit(credentialRef)
        val requestBody = mapOf(
            "query" to PULL_REQUEST_QUERY,
            "variables" to mapOf("owner" to owner, "name" to name, "cursor" to cursor)
        )

        val response = restClient.post()
            .uri("/graphql")
            .header("Authorization", "Bearer $token")
            .body(requestBody)
            .retrieve()
            .body(object : ParameterizedTypeReference<GraphQLResponse<PullRequestsResponse>>() {})
            ?: throw GitHubApiException("Empty response from GitHub API")

        val unwrappedResponse = response.unwrap()

        lastKnownRateLimit[credentialRef] = unwrappedResponse.rateLimit

        return unwrappedResponse.repository?.pullRequests ?: throw GitHubApiException("Empty response from GitHub API")
    }

    fun fetchIssues(userId: UUID, owner: String, name: String, credentialRef: UUID, cursor: String?): IssueLikeConnection {
        val token = credentialService.getTokenForUser(credentialRef, userId)
        awaitRateLimit(credentialRef)
        val requestBody = mapOf(
            "query" to ISSUES_QUERY,
            "variables" to mapOf("owner" to owner, "name" to name, "cursor" to cursor)
        )

        val response = restClient.post()
            .uri("/graphql")
            .header("Authorization", "Bearer $token")
            .body(requestBody)
            .retrieve()
            .body(object : ParameterizedTypeReference<GraphQLResponse<IssuesResponse>>() {})
            ?: throw GitHubApiException("Empty response from GitHub API")

        val unwrappedResponse = response.unwrap()

        lastKnownRateLimit[credentialRef] = unwrappedResponse.rateLimit

        return unwrappedResponse.repository?.issues ?: throw GitHubApiException("Empty response from GitHub API")
    }

    // Asks GitHub directly and does not go through awaitRateLimit: the answer matters most when few points are left.
    fun fetchRateLimit(userId: UUID, credentialRef: UUID): RateLimitInfo {
        val token = credentialService.getTokenForUser(credentialRef, userId)
        val requestBody = mapOf("query" to RATE_LIMIT_QUERY)

        val response = restClient.post()
            .uri("/graphql")
            .header("Authorization", "Bearer $token")
            .body(requestBody)
            .retrieve()
            .body(object : ParameterizedTypeReference<GraphQLResponse<RateLimitResponse>>() {})
            ?: throw GitHubApiException("Empty response from GitHub API")

        val rateLimit = response.unwrap().rateLimit
            ?: throw GitHubApiException("GitHub did not report a rate limit")

        lastKnownRateLimit[credentialRef] = rateLimit

        return rateLimit
    }

    fun getLastKnownRateLimit(credentialRef: UUID): RateLimitInfo? = lastKnownRateLimit[credentialRef]

    // Holds a request back until GitHub resets the limit when the points of this credential are nearly used up,
    // so a long backfill slows down instead of failing on a refusal.
    private fun awaitRateLimit(credentialRef: UUID) {
        val rateLimit = lastKnownRateLimit[credentialRef] ?: return
        if (rateLimit.remaining > RATE_LIMIT_RESERVE) return

        val wait = Duration.between(clock.instant(), rateLimit.resetAt).plus(RESET_MARGIN)
        if (wait <= Duration.ZERO) return

        logger.info("GitHub rate limit nearly used up ({} points left), waiting {}s for the reset", rateLimit.remaining, wait.toSeconds())
        pause(wait)
    }

    // Separate from awaitRateLimit so that tests can replace the real sleep.
    protected fun pause(duration: Duration) {
        Thread.sleep(duration)
    }
}