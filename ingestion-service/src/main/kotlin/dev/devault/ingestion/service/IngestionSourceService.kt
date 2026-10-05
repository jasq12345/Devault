package dev.devault.ingestion.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.dto.response.IngestionSourceResponseDto
import dev.devault.ingestion.dto.response.toResponse
import dev.devault.ingestion.exception.SourceAlreadyConnectedException
import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestionSourceRepository
import dev.devault.ingestion.type.StatusType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IngestionSourceService(
    private val repository: IngestionSourceRepository,
    private val credentialService: CredentialService,
    private val backfillService: BackfillService
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
}