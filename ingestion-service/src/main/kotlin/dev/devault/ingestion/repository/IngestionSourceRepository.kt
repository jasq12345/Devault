package dev.devault.ingestion.repository

import dev.devault.ingestion.model.IngestionSource
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IngestionSourceRepository : JpaRepository<IngestionSource, UUID> {
    fun findByIdAndWorkspaceId(sourceId: UUID, workspaceId: UUID): IngestionSource?
    fun existsByCredentialRef(credentialRef: UUID): Boolean
    fun findAllByWorkspaceId(workspaceId: UUID): List<IngestionSource>
}