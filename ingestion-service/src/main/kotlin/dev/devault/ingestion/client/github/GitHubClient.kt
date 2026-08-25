package dev.devault.ingestion.client.github

import dev.devault.ingestion.client.github.dto.GraphQLResponse
import dev.devault.ingestion.client.github.dto.HistoryConnection
import dev.devault.ingestion.client.github.dto.RepositoryHistoryResponse
import dev.devault.ingestion.exception.GitHubApiException
import dev.devault.ingestion.service.CredentialService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
import java.util.UUID

class GitHubClient(
    @Qualifier("gitHubRestClient") private val restClient: RestClient,
    private val credentialService: CredentialService,
    ) {

    companion object{
        private const val COMMIT_HISTORY_QUERY: String = """
            query(${'$'}owner: String!, ${'$'}name: String!, ${'$'}cursor: String) {
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
    }
    fun fetchCommitHistory(owner: String, name: String, credentialRef: UUID, cursor: String?): HistoryConnection {
        val token = credentialService.getToken(credentialRef)
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

        return response.unwrap().repository.defaultBranchRef?.target?.history
            ?: throw GitHubApiException("No commit history found")
    }
}