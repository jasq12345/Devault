package dev.devault.ingestion.dto.response

import java.util.UUID

data class CredentialResponseDto(
    val id: UUID,
    val label: String
)