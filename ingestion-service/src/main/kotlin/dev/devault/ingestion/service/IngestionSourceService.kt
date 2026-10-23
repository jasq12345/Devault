package dev.devault.ingestion.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.type.WorkspaceRole
import dev.devault.ingestion.client.workspace.WorkspaceClient
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.dto.response.IngestionSourceResponseDto
import dev.devault.ingestion.dto.response.toResponse
import dev.devault.ingestion.exception.InsufficientPermissionException
import dev.devault.ingestion.exception.SourceAlreadyConnectedException
import dev.devault.ingestion.exception.SourceNotFoundException
import dev.devault.ingestion.exception.SourceSyncInProgressException
import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestionSourceRepository
import dev.devault.ingestion.type.StatusType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IngestionSourceService(
    private val repository: IngestionSourceRepository,
    private val credentialService: CredentialService,
    private val backfillService: BackfillService,
    private val documentService: IngestedDocumentService,
    private val workspaceClient: WorkspaceClient
) {
    fun connectSource(authenticatedUser: AuthenticatedUser, workspaceId: UUID, dto: SaveIngestionSourceDto): IngestionSourceResponseDto {
        requireRole(workspaceId, setOf(WorkspaceRole.ADMIN, WorkspaceRole.OWNER))
        credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id)

        val source = IngestionSource(
            workspaceId = workspaceId,
            externalId = "${dto.owner}/${dto.name}".lowercase(),
            credentialRef = dto.credentialRef,
            connectedByUserId = authenticatedUser.id,
            status = StatusType.PENDING
        )

        val savedSource = try {
            repository.saveAndFlush(source)
        } catch (_: DataIntegrityViolationException) {
            throw SourceAlreadyConnectedException("Source is already connected to this workspace")
        }
        backfillService.runBackfill(savedSource.id!!)

        return savedSource.toResponse()
    }

    fun findAllIngestedSources(workspaceId: UUID): List<IngestionSourceResponseDto> {
        requireMember(workspaceId)

        val sources = repository.findAllByWorkspaceId(workspaceId)

        return sources.map { it.toResponse() }
    }

    fun findIngestedSource(workspaceId: UUID, sourceId: UUID): IngestionSourceResponseDto {
        requireMember(workspaceId)
        return findSource(workspaceId, sourceId).toResponse()
    }

    @Transactional
    fun deleteIngestedSource(workspaceId: UUID, sourceId: UUID) {
        requireRole(workspaceId, setOf(WorkspaceRole.ADMIN, WorkspaceRole.OWNER))
        val source = findSource(workspaceId, sourceId)

        if (source.status in listOf(StatusType.PENDING, StatusType.SYNCING))
            throw SourceSyncInProgressException("Source is being synchronized")

        documentService.deleteAllIngestedDocuments(source)
        repository.delete(source)
    }

    fun isCredentialInUse(credentialRef: UUID): Boolean {
        return repository.existsByCredentialRef(credentialRef)
    }

    private fun findSource(workspaceId: UUID, sourceId: UUID): IngestionSource {
        return repository.findByIdAndWorkspaceId(
            sourceId = sourceId,
            workspaceId = workspaceId
        ) ?: throw SourceNotFoundException("Source not found")
    }

    private fun requireRole(workspaceId: UUID, allowed: Set<WorkspaceRole>) {
        val role = workspaceClient.getWorkspaceMemberRole(workspaceId)
        if (role !in allowed)
            throw InsufficientPermissionException("Insufficient member permission")
    }

    private fun requireMember(workspaceId: UUID) {
        workspaceClient.getWorkspaceMemberRole(workspaceId)
    }
}
