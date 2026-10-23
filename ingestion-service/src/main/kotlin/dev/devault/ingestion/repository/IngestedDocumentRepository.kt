package dev.devault.ingestion.repository

import dev.devault.ingestion.model.IngestedDocument
import dev.devault.ingestion.model.IngestionSource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface IngestedDocumentRepository : JpaRepository<IngestedDocument, UUID> {
    fun existsBySourceAndExternalRef(source: IngestionSource, externalRef: String): Boolean

    @Modifying
    @Query("delete from IngestedDocument d where d.source = :source")
    fun deleteAllBySource(@Param("source") source: IngestionSource): Int
}