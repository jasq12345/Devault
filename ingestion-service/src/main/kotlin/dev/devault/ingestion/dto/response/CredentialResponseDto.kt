package dev.devault.ingestion.dto.response

import dev.devault.ingestion.model.Credential
import java.util.UUID

data class CredentialResponseDto(
    val id: UUID,
    val label: String
)

fun Credential.toResponse() = CredentialResponseDto(
    id = id!!,
    label = label,
)