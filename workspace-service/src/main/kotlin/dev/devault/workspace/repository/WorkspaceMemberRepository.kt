package dev.devault.workspace.repository

import dev.devault.workspace.model.WorkspaceMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WorkspaceMemberRepository : JpaRepository<WorkspaceMember, UUID> {

}