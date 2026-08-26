package dev.devault.ingestion.client.github.dto

data class IssuesResponse(val repository: RepositoryIssues)
data class RepositoryIssues(val issues: IssueLikeConnection)