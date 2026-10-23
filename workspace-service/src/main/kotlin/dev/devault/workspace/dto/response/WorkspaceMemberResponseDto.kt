package dev.devault.workspace.dto.response

import dev.devault.workspace.model.WorkspaceMember
import dev.devault.commonlib.type.WorkspaceRole
import java.util.UUID

data class WorkspaceMemberResponseDto(
    val id: UUID?,
    val userId: UUID,
    val role: WorkspaceRole,
    val workspaceId: UUID?
)

fun WorkspaceMember.toResponse() = WorkspaceMemberResponseDto(
    id = id,
    userId = userId,
    role = role,
    workspaceId = workspace.id
)