package dev.devault.ingestion.client.github.dto

import dev.devault.ingestion.exception.GitHubApiException

data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null
) {
    fun unwrap(): T {
        if (!errors.isNullOrEmpty()) {
            throw GitHubApiException(errors.joinToString("; ") { it.message })
        }
        return data ?: throw GitHubApiException("GraphQL response missing data")
    }
}
