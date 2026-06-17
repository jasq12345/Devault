package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.dto.request.SaveWorkspaceDto
import dev.devault.workspace.dto.request.UpdateWorkspaceDto
import dev.devault.workspace.dto.response.WorkspaceResponseDto
import dev.devault.workspace.dto.response.toResponse
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.repository.WorkspaceRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkspaceService(
    private val repository: WorkspaceRepository
) {
    fun findAllWorkspaces(authenticatedUser: AuthenticatedUser): MutableList<WorkspaceResponseDto> {
        val workspaces = repository.getWorkspaceByOwnerId(authenticatedUser.id)

        return workspaces.map { it.toResponse() }.toMutableList()
    }

    fun findWorkspaceById(authenticatedUser: AuthenticatedUser, id: UUID): WorkspaceResponseDto {
        val workspace = repository.findById(id)
            .orElseThrow { NoSuchElementException("Workspace not found") }

        if(workspace.ownerId != authenticatedUser.id)
            throw AccessDeniedException("Access denied")

        return workspace.toResponse()
    }

    fun saveWorkspace(authenticatedUser: AuthenticatedUser, dto: SaveWorkspaceDto): WorkspaceResponseDto {
        if(repository.existsBySlug(dto.slug))
            throw IllegalStateException("Workspace with this slug already exists")

        val workspace = Workspace(
            name = dto.name,
            slug = dto.slug,
            ownerId = authenticatedUser.id
        )

        return repository.save(workspace).toResponse()
    }

    fun updateWorkspace(authenticatedUser: AuthenticatedUser, dto: UpdateWorkspaceDto, id: UUID): WorkspaceResponseDto {
        val workspace = repository.findWorkspaceByIdAndOwnerId(id, authenticatedUser.id)
            ?: throw AccessDeniedException("Access denied")

        workspace.name = dto.name

        return repository.save(workspace).toResponse()
    }

    fun deleteWorkspace(authenticatedUser: AuthenticatedUser, id: UUID) {
        val workspace = repository.findWorkspaceByIdAndOwnerId(id, authenticatedUser.id)
            ?: throw AccessDeniedException("Access denied")

        repository.delete(workspace)
    }
}