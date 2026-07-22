package dev.devault.workspace.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class SaveWorkspaceMemberDto(
    @NotNull(message = "userId must not be null")
    var userId: UUID,
)