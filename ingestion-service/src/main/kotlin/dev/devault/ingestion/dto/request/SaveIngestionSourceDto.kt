package dev.devault.ingestion.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

data class SaveIngestionSourceDto(
    @NotBlank
    @Schema(description = "GitHub repository owner", example = "spring-projects")
    @Pattern(
        regexp = "^[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$",
        message = "Owner can only contain letters, digits and single hyphens, and cannot start or end with a hyphen"
    )
    val owner: String,

    @NotBlank
    @Schema(description = "GitHub repository name", example = "spring-boot")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]{1,100}$",
        message = "Name can only contain letters, digits, dots, hyphens and underscores"
    )
    val name: String,

    @Schema(description = "ID of a credential created via POST /credentials", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val credentialRef: UUID
)