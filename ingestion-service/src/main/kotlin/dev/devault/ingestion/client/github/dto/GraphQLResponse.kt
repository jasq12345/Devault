package dev.devault.ingestion.client.github.dto

import dev.devault.ingestion.exception.GithubApiException

data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null
) {
    fun unwrap(): T {
        if (!errors.isNullOrEmpty()) {
            throw GithubApiException(errors.joinToString("; ") { it.message })
        }
        return data ?: throw GithubApiException("GraphQL response missing data")
    }
}
