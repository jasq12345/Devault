package dev.devault.ingestion.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.dto.response.IngestionSourceResponseDto
import dev.devault.ingestion.dto.response.toResponse
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
    private val documentService: IngestedDocumentService
) {
    fun connectSource(authenticatedUser: AuthenticatedUser, workspaceId: UUID, dto: SaveIngestionSourceDto): IngestionSourceResponseDto {
        // TODO(ING-9): brak weryfikacji roli w workspace

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

    fun findAllIngestedSources(authenticatedUser: AuthenticatedUser, workspaceId: UUID): List<IngestionSourceResponseDto> {
        // TODO(ING-9): filtr po użytkowniku zastępuje brak weryfikacji członkostwa w workspace
        val sources = repository.findAllByConnectedByUserIdAndWorkspaceId(authenticatedUser.id, workspaceId)

        return sources.map { it.toResponse() }
    }

    fun findIngestedSource(authenticatedUser: AuthenticatedUser, workspaceId: UUID, sourceId: UUID): IngestionSourceResponseDto {
        return findSource(authenticatedUser, workspaceId, sourceId).toResponse()
    }

    @Transactional
    fun deleteIngestedSource(authenticatedUser: AuthenticatedUser, workspaceId: UUID, sourceId: UUID) {
        val source = findSource(authenticatedUser, workspaceId, sourceId)

        if (source.status in listOf(StatusType.PENDING, StatusType.SYNCING))
            throw SourceSyncInProgressException("Source is being synchronized")

        documentService.deleteAllIngestedDocuments(source)
        repository.delete(source)
    }

    fun isCredentialInUse(credentialRef: UUID): Boolean {
        return repository.existsByCredentialRef(credentialRef)
    }

    private fun findSource(authenticatedUser: AuthenticatedUser, workspaceId: UUID, sourceId: UUID): IngestionSource {
        // TODO(ING-9): filtr po użytkowniku zastępuje brak weryfikacji członkostwa w workspace
        return repository.findByConnectedByUserIdAndIdAndWorkspaceId(
            userId = authenticatedUser.id,
            sourceId = sourceId,
            workspaceId = workspaceId
        ) ?: throw SourceNotFoundException("Source not found")
    }
}
