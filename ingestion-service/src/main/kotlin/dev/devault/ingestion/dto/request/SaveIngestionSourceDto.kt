package dev.devault.ingestion.dto.request

import jakarta.validation.constraints.NotBlank

data class SaveIngestionSourceDto(
    @NotBlank
    val owner: String,

    @NotBlank
    val name: String,

    @NotBlank
    val token: String
)