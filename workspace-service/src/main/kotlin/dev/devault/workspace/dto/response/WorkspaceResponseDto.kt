package dev.devault.workspace.dto.response

import dev.devault.workspace.model.Workspace
import java.util.UUID

data class WorkspaceResponseDto(
    val id: UUID?,
    val name: String,
    val slug: String,
    val ownerId: UUID
)

fun Workspace.toResponse() = WorkspaceResponseDto(
    id = id,
    name = name,
    slug = slug,
    ownerId = ownerId
)