package dev.devault.workspace.repository

import dev.devault.workspace.model.Workspace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WorkspaceRepository : JpaRepository<Workspace, UUID>{
    fun getWorkspaceByOwnerId(ownerId: UUID): MutableList<Workspace>
}