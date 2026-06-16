package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.dto.request.SaveWorkspaceMemberDto
import dev.devault.workspace.model.WorkspaceMember
import dev.devault.workspace.repository.WorkspaceMemberRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkspaceMemberService(
    private val repository: WorkspaceMemberRepository
) {
    fun findAllMembers(authenticatedUser: AuthenticatedUser, workspaceId: UUID): MutableList<WorkspaceMember> =
        repository.findWorkspaceMemberByWorkspaceIdAndWorkspaceOwnerId(workspaceId, authenticatedUser.id)

    fun findWorkspaceMemberById(authenticatedUser: AuthenticatedUser, workspaceId: UUID, id: UUID): WorkspaceMember {
        val member = repository.findWorkspaceMemberByIdAndWorkspaceId(id, workspaceId)
            ?: throw NoSuchElementException("Workspace member not found")

        if(member.workspace.ownerId != authenticatedUser.id)
            throw AccessDeniedException("Access denied")

        return member
    }

    fun saveWorkspaceMember(authenticatedUser: AuthenticatedUser, workspaceId: UUID, dto: SaveWorkspaceMemberDto): WorkspaceMember? {
        return null
    }

}