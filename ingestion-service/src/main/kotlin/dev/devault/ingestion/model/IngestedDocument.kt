package dev.devault.ingestion.model

import dev.devault.ingestion.type.DocumentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "ingested_documents",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["source_id", "external_ref"])
    ]
)
class IngestedDocument(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne
    @JoinColumn(name = "source_id")
    var source: IngestionSource,

    @Column(nullable = false)
    var externalRef: String,

    @Column(nullable = false)
    var contentHash: String,

    @Column(nullable = false)
    var rawContent: String,

    @Enumerated(EnumType.STRING)
    var documentType: DocumentType,

    var ingestedAt: Instant? = null
)