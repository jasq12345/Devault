package dev.devault.workspace.dto.request

import dev.devault.workspace.type.WorkspaceRole
import jakarta.validation.constraints.NotNull

data class UpdateWorkspaceMemberRoleDto(
    @NotNull(message = "role must not be null")
    var role: WorkspaceRole
)