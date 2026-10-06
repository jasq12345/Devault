package dev.devault.ingestion.repository

import dev.devault.ingestion.model.IngestionSource
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IngestionSourceRepository : JpaRepository<IngestionSource, UUID> {
    fun findAllByConnectedByUserIdAndWorkspaceId(userId: UUID, workspaceId: UUID): List<IngestionSource>
    fun findByConnectedByUserIdAndIdAndWorkspaceId(userId: UUID, sourceId: UUID, workspaceId: UUID): IngestionSource?
}