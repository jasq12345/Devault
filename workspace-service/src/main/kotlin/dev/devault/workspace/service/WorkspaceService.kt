package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.dto.request.SaveWorkspaceDto
import dev.devault.workspace.dto.request.UpdateWorkspaceDto
import dev.devault.workspace.dto.response.WorkspaceResponseDto
import dev.devault.workspace.dto.response.toResponse
import dev.devault.workspace.exception.SlugAlreadyExistsException
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.repository.WorkspaceRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WorkspaceService(
    private val repository: WorkspaceRepository,
    private val workspaceMemberService: WorkspaceMemberService
) {
    fun findAllWorkspaces(authenticatedUser: AuthenticatedUser): List<WorkspaceResponseDto> {
        val memberships = workspaceMemberService.findWorkspacesByUserId(authenticatedUser.id)

        return memberships.map { it.workspace.toResponse() }.toList()
    }

    fun findWorkspaceById(authenticatedUser: AuthenticatedUser, id: UUID): WorkspaceResponseDto {
        val workspace = repository.findById(id)
            .orElseThrow { NoSuchElementException("Workspace not found") }

        if(!workspaceMemberService.isMember(id, authenticatedUser.id))
            throw AccessDeniedException("Access denied")

        return workspace.toResponse()
    }

    @Transactional
    fun saveWorkspace(authenticatedUser: AuthenticatedUser, dto: SaveWorkspaceDto): WorkspaceResponseDto {
        if(repository.existsBySlug(dto.slug))
            throw SlugAlreadyExistsException("Workspace with this slug already exists")

        val workspace = Workspace(
            name = dto.name,
            slug = dto.slug,
            ownerId = authenticatedUser.id
        )

        val savedWorkspace = repository.save(workspace)

        workspaceMemberService.addOwner(savedWorkspace, authenticatedUser.id)

        return savedWorkspace.toResponse()
    }

    fun updateWorkspace(authenticatedUser: AuthenticatedUser, dto: UpdateWorkspaceDto, id: UUID): WorkspaceResponseDto {
        val workspace = repository.findById(id)
            .orElseThrow { NoSuchElementException("Workspace not found") }

        if (workspace.ownerId != authenticatedUser.id)
            throw AccessDeniedException("Access denied")

        workspace.name = dto.name
        return repository.save(workspace).toResponse()
    }

    fun deleteWorkspace(authenticatedUser: AuthenticatedUser, id: UUID) {
        val workspace = repository.findById(id)
            .orElseThrow { NoSuchElementException("Workspace not found") }

        if (workspace.ownerId != authenticatedUser.id)
            throw AccessDeniedException("Access denied")

        repository.delete(workspace)
    }
}