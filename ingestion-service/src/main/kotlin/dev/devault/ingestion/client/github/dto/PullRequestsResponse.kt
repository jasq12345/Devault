package dev.devault.ingestion.client.github.dto

data class PullRequestsResponse(
    val rateLimit: RateLimitInfo,
    val repository: RepositoryPullRequests
)
data class RepositoryPullRequests(val pullRequests: IssueLikeConnection)