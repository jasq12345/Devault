package dev.devault.workspace.dto.request

import dev.devault.workspace.type.WorkspaceRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UpdateWorkspaceMemberRoleDto(
    @NotNull(message = "role must not be null")
    @Schema(example = "ADMIN")
    var role: WorkspaceRole
)