package dev.devault.ingestion.service

import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestedDocumentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class IngestedDocumentServiceTest {
    private val documentRepository = mockk<IngestedDocumentRepository>()
    private val service = IngestedDocumentService(documentRepository)

    @Test
    fun `deletes all documents of the given source with one bulk query`() {
        val source = IngestionSource(
            id = UUID.randomUUID(),
            workspaceId = UUID.randomUUID(),
            externalId = "octo/repo",
            credentialRef = UUID.randomUUID(),
            connectedByUserId = UUID.randomUUID()
        )
        every { documentRepository.deleteAllBySource(source) } returns 3

        service.deleteAllIngestedDocuments(source)

        verify(exactly = 1) { documentRepository.deleteAllBySource(source) }
    }
}
