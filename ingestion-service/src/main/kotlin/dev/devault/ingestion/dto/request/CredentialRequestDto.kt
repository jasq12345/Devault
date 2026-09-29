package dev.devault.ingestion.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class CredentialRequestDto(
    @NotBlank
    @Schema(example = "My GitHub token")
    val label: String,

    @NotBlank
    @Schema(description = "GitHub personal access token", example = "ghp_exampleToken0123456789abcdefghijklmn")
    val token: String
)