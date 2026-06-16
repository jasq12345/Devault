package dev.devault.workspace.repository

import dev.devault.workspace.model.WorkspaceMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WorkspaceMemberRepository : JpaRepository<WorkspaceMember, UUID> {
    fun findWorkspaceMemberByWorkspaceIdAndWorkspaceOwnerId(id: UUID, ownerId: UUID): MutableList<WorkspaceMember>

    fun findWorkspaceMemberByIdAndWorkspaceId(id: UUID, workspaceId: UUID): WorkspaceMember?
}