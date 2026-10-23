package dev.devault.ingestion.service

import dev.devault.ingestion.exception.CredentialInUseException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Deleting a credential touches credentials and sources. It lives in its own class, because
 * IngestionSourceService already depends on CredentialService and the reverse would be a cycle.
 */
@Service
class CredentialDeletionService(
    private val credentialService: CredentialService,
    private val sourceService: IngestionSourceService
) {
    @Transactional
    fun delete(id: UUID, userId: UUID) {
        credentialService.requireOwned(id, userId)

        // credentialRef has no foreign key on purpose, so the database would not stop this delete.
        if (sourceService.isCredentialInUse(id))
            throw CredentialInUseException("Credential is used by an ingestion source")

        credentialService.delete(id, userId)
    }
}
