package dev.devault.ingestion.dto.response

import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.type.SourceType
import dev.devault.ingestion.type.StatusType
import java.time.Instant
import java.util.UUID

data class IngestionSourceResponseDto(
    val id: UUID,
    val workspaceId: UUID,
    val source: SourceType,
    val externalId: String,
    val connectedByUserId: UUID,
    val status: StatusType,
    val lastSynced: Instant?
)

fun IngestionSource.toResponse() = IngestionSourceResponseDto(
    id = id!!,
    workspaceId = workspaceId,
    source = source,
    externalId = externalId,
    connectedByUserId = connectedByUserId,
    status = status,
    lastSynced = lastSynced
)