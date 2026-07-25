package dev.devault.ingestion.repository

import dev.devault.ingestion.model.IngestedDocument
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IngestedDocumentRepository : JpaRepository<IngestedDocument, UUID> {

}