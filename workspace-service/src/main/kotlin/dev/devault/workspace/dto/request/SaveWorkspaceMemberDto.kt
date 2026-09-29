package dev.devault.workspace.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class SaveWorkspaceMemberDto(
    @NotNull(message = "userId must not be null")
    @Schema(description = "ID of an existing user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    var userId: UUID,
)