package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.repository.WorkspaceRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkspaceService(
    private val repository: WorkspaceRepository
) {
    fun findAllWorkspaces(authenticatedUser: AuthenticatedUser): MutableList<Workspace> =
        repository.getWorkspaceByOwnerId(authenticatedUser.id)


    fun findWorkspaceById(authenticatedUser: AuthenticatedUser, id: UUID): Workspace {
        val workspace = repository.findById(id)
            .orElseThrow { NoSuchElementException("Workspace not found") }

        if(workspace.ownerId != authenticatedUser.id)
            throw AccessDeniedException("Access denied")

        return workspace
    }

}