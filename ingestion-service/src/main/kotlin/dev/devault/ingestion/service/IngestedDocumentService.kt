package dev.devault.ingestion.service

import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestedDocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IngestedDocumentService(
    private val documentRepository: IngestedDocumentRepository
) {

    @Transactional
    fun deleteAllIngestedDocuments(source: IngestionSource) {
        documentRepository.deleteAllBySource(source)
    }
}