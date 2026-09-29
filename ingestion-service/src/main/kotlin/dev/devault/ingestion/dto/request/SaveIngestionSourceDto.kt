package dev.devault.ingestion.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class SaveIngestionSourceDto(
    @NotBlank
    @Schema(description = "GitHub repository owner", example = "spring-projects")
    val owner: String,

    @NotBlank
    @Schema(description = "GitHub repository name", example = "spring-boot")
    val name: String,

    @Schema(description = "ID of a credential created via POST /credentials", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val credentialRef: UUID
)