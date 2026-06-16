package dev.devault.workspace.dto.request

import dev.devault.workspace.type.WorkspaceRole
import java.util.UUID

data class SaveWorkspaceMemberDto(
    val userId: UUID,
    val role: WorkspaceRole,
)
