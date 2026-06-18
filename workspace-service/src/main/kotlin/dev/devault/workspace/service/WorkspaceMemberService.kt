package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.dto.request.SaveWorkspaceMemberDto
import dev.devault.workspace.dto.request.UpdateWorkspaceMemberRoleDto
import dev.devault.workspace.dto.response.WorkspaceMemberResponseDto
import dev.devault.workspace.dto.response.toResponse
import dev.devault.workspace.exception.CannotRemoveProtectedRoleException
import dev.devault.workspace.exception.UserAlreadyMemberException
import dev.devault.workspace.model.Workspace
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
    fun findAllMembers(authenticatedUser: AuthenticatedUser, workspaceId: UUID): MutableList<WorkspaceMemberResponseDto> {
        val members = repository.findAllByWorkspaceId(workspaceId)
        if (members.isEmpty())
            throw NoSuchElementException("Workspace not found")

        if (members.none { it.userId == authenticatedUser.id })
            throw AccessDeniedException("Access denied")

        return members.map { it.toResponse() }.toMutableList()
    }

    fun findWorkspaceMemberById(authenticatedUser: AuthenticatedUser, workspaceId: UUID, id: UUID): WorkspaceMemberResponseDto {
        if (!repository.existsByWorkspaceId(workspaceId))
            throw NoSuchElementException("Workspace not found")

        if (!repository.existsByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id))
            throw AccessDeniedException("Access denied")

        val member = repository.findByIdAndWorkspaceId(id, workspaceId)
            ?: throw NoSuchElementException("Workspace member not found")

        return member.toResponse()
    }

    fun saveWorkspaceMember(authenticatedUser: AuthenticatedUser, workspaceId: UUID, dto: SaveWorkspaceMemberDto): WorkspaceMemberResponseDto {
        val members = repository.findAllByWorkspaceId(workspaceId)
        if (members.isEmpty())
            throw NoSuchElementException("Workspace not found")

        if (members.none { it.userId == authenticatedUser.id && it.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN) })
            throw AccessDeniedException("Access denied")

        if (members.any { it.userId == dto.userId })
            throw UserAlreadyMemberException("User is already a member")

        val member = WorkspaceMember(
            userId = dto.userId,
            role = WorkspaceRole.MEMBER,
            workspace = members.first().workspace
        )
        return repository.save(member).toResponse()
    }

    fun deleteWorkspaceMember(authenticatedUser: AuthenticatedUser, workspaceId: UUID, id: UUID) {
        requireRole(workspaceId, authenticatedUser.id, listOf(WorkspaceRole.ADMIN, WorkspaceRole.OWNER))

        val member = repository.findByIdAndWorkspaceId(id, workspaceId)
            ?: throw NoSuchElementException("Workspace member not found")

        if(member.role in listOf(WorkspaceRole.ADMIN, WorkspaceRole.OWNER))
            throw CannotRemoveProtectedRoleException("Cannot remove an admin or owner")

        repository.delete(member)
    }

    private fun requireRole(workspaceId: UUID, userId: UUID, roles: List<WorkspaceRole>): WorkspaceMember {
        if (!repository.existsByWorkspaceId(workspaceId))
            throw NoSuchElementException("Workspace not found")

        val member = repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, userId)
            ?: throw AccessDeniedException("Access denied")

        if (member.role !in roles)
            throw AccessDeniedException("Access denied")

        return member
    }

    fun addOwner(workspace: Workspace, userId: UUID): WorkspaceMember {
        return repository.save(
            WorkspaceMember(workspace = workspace, userId = userId, role = WorkspaceRole.OWNER)
        )
    }

    fun findWorkspacesByUserId(userId: UUID): MutableList<WorkspaceMember> {
        return repository.findByUserId(userId)
    }

    fun isMember(workspaceId: UUID, userId: UUID): Boolean {
        return repository.existsByWorkspaceIdAndUserId(workspaceId, userId)
    }

    fun updateMemberRole(
        authenticatedUser: AuthenticatedUser,
        workspaceId: UUID,
        id: UUID,
        dto: UpdateWorkspaceMemberRoleDto
    ): WorkspaceMemberResponseDto {


    }

}