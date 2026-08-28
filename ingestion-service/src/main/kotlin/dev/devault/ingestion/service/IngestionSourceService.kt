package dev.devault.ingestion.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.dto.response.IngestionSourceResponseDto
import dev.devault.ingestion.repository.IngestionSourceRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IngestionSourceService(
    private val repository: IngestionSourceRepository,
    private val backfillService: BackfillService,
    private val credentialService: CredentialService
) {
    fun connectSource(authenticatedUser: AuthenticatedUser, workspaceId: UUID, dto: SaveIngestionSourceDto) {
        // TODO(ING-9): brak weryfikacji roli w workspace — na razie każdy zalogowany user
        // może podłączyć źródło do dowolnego workspace'u, o ile zna jego workspaceId.
        // Docelowo: REST do workspace-service, sprawdzić ADMIN/OWNER (analogicznie do requireRole()).
    }

}