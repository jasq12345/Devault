package dev.devault.ingestion.dto.request

import jakarta.validation.constraints.NotBlank

data class CredentialRequestDto(
    @NotBlank
    val label: String,

    @NotBlank
    val token: String
)