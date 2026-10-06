package dev.devault.ingestion.client.github

import dev.devault.ingestion.client.github.dto.GraphQLResponse
import dev.devault.ingestion.client.github.dto.HistoryConnection
import dev.devault.ingestion.client.github.dto.IssueLikeConnection
import dev.devault.ingestion.client.github.dto.IssuesResponse
import dev.devault.ingestion.client.github.dto.PullRequestsResponse
import dev.devault.ingestion.client.github.dto.RateLimitInfo
import dev.devault.ingestion.client.github.dto.RepositoryHistoryResponse
import dev.devault.ingestion.exception.GitHubApiException
import dev.devault.ingestion.service.CredentialService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.util.UUID

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
    ) {
    @Volatile
    private var lastKnownRateLimit: RateLimitInfo? = null

    companion object{
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


    }
    fun fetchCommitHistory(userId: UUID, owner: String, name: String, credentialRef: UUID, cursor: String?): HistoryConnection {
        val token = credentialService.getTokenForUser(credentialRef, userId)
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

        lastKnownRateLimit = unwrappedResponse.rateLimit

        return unwrappedResponse.repository?.defaultBranchRef?.target?.history
            ?: throw GitHubApiException("No commit history found")
    }

    fun fetchPullRequests(userId: UUID, owner: String, name: String, credentialRef: UUID, cursor: String?): IssueLikeConnection {
        val token = credentialService.getTokenForUser(credentialRef, userId)
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

        lastKnownRateLimit = unwrappedResponse.rateLimit

        return unwrappedResponse.repository?.pullRequests ?: throw GitHubApiException("Empty response from GitHub API")
    }

    fun fetchIssues(userId: UUID, owner: String, name: String, credentialRef: UUID, cursor: String?): IssueLikeConnection {
        val token = credentialService.getTokenForUser(credentialRef, userId)
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

        lastKnownRateLimit = unwrappedResponse.rateLimit

        return unwrappedResponse.repository?.issues ?: throw GitHubApiException("Empty response from GitHub API")
    }

    fun getLastKnownRateLimit(): RateLimitInfo? = lastKnownRateLimit
}