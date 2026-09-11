package dev.devault.ingestion.client.github.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RepositoryHistoryResponse(
    val rateLimit: RateLimitInfo,
    val repository: Repository
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Repository(
    val defaultBranchRef: BranchRef?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BranchRef(
    val target: CommitTarget?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommitTarget(
    val history: HistoryConnection
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HistoryConnection(
    val pageInfo: PageInfo,
    val nodes: List<CommitNode>
)