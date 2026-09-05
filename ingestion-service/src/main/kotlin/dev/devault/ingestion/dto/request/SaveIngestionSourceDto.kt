package dev.devault.ingestion.dto.request

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class SaveIngestionSourceDto(
    @NotBlank
    val owner: String,

    @NotBlank
    val name: String,

    @NotBlank
    val credentialRef: UUID
)