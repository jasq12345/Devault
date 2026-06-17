package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.dto.request.SaveWorkspaceMemberDto
import dev.devault.workspace.model.WorkspaceMember
import dev.devault.workspace.repository.WorkspaceMemberRepository
import dev.devault.workspace.type.WorkspaceRole
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkspaceMemberService(
    private val repository: WorkspaceMemberRepository
) {
    fun findAllMembers(authenticatedUser: AuthenticatedUser, workspaceId: UUID): MutableList<WorkspaceMember> {
        val members = repository.findAllByWorkspaceId(workspaceId)
        if (members.none { it.userId == authenticatedUser.id })
            throw AccessDeniedException("Access denied")
        return members
    }

    fun findWorkspaceMemberById(authenticatedUser: AuthenticatedUser, workspaceId: UUID, id: UUID): WorkspaceMember {
        if (!repository.existsByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id))
            throw AccessDeniedException("Access denied")

        return repository.findByIdAndWorkspaceId(id, workspaceId)
            ?: throw NoSuchElementException("Workspace member not found")
    }

    fun saveWorkspaceMember(authenticatedUser: AuthenticatedUser, workspaceId: UUID, dto: SaveWorkspaceMemberDto): WorkspaceMember {
        val members = repository.findAllByWorkspaceId(workspaceId)

        if (members.none { it.userId == authenticatedUser.id && it.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN) })
            throw AccessDeniedException("Access denied")

        if (members.any { it.userId == dto.userId })
            throw IllegalStateException("User is already a member")

        val member = WorkspaceMember(
            userId = dto.userId,
            role = WorkspaceRole.MEMBER,
            workspace = members.first().workspace
        )

        return repository.save(member)
    }

    fun deleteWorkspaceMember(authenticatedUser: AuthenticatedUser, workspaceId: UUID, id: UUID) {
        requireRole(workspaceId, authenticatedUser.id, listOf(WorkspaceRole.ADMIN, WorkspaceRole.OWNER))

        val member = repository.findByIdAndWorkspaceId(id, workspaceId)
            ?: throw NoSuchElementException("Workspace member not found")

        if(member.role in listOf(WorkspaceRole.ADMIN, WorkspaceRole.OWNER))
            throw IllegalStateException("Cannot remove an admin or owner")

        repository.delete(member)
    }

    private fun requireRole(workspaceId: UUID, userId: UUID, roles: List<WorkspaceRole>): WorkspaceMember {
        val member = repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, userId)
            ?: throw AccessDeniedException("Access denied")

        if (member.role !in roles)
            throw AccessDeniedException("Access denied")
        return member
    }
}