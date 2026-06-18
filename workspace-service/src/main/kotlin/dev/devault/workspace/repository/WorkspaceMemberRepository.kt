package dev.devault.workspace.repository

import dev.devault.workspace.model.WorkspaceMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WorkspaceMemberRepository : JpaRepository<WorkspaceMember, UUID> {
    fun findAllByWorkspaceId(workspaceId: UUID): MutableList<WorkspaceMember>
    fun existsByWorkspaceIdAndUserId(workspaceId: UUID, userId: UUID): Boolean
    fun findByIdAndWorkspaceId(id: UUID, workspaceId: UUID): WorkspaceMember?
    fun findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId: UUID, id: UUID): WorkspaceMember?
    fun findByUserId(userId: UUID): MutableList<WorkspaceMember>
    fun existsByWorkspaceId(workspaceId: UUID): Boolean
}