package dev.devault.ingestion.client.workspace.dto

// The member returned by workspace-service, reduced to what ingestion-service needs.
data class WorkspaceMemberResponse(
    val role: String
)
