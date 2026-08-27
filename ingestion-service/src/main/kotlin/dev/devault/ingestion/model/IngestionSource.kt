package dev.devault.ingestion.model

import dev.devault.ingestion.type.SourceType
import dev.devault.ingestion.type.StatusType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "ingestion_sources",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["workspace_id", "source", "external_id"])
    ]
)
class IngestionSource(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var workspaceId: UUID,

    @Enumerated(EnumType.STRING)
    var source: SourceType = SourceType.GITHUB,

    @Column(nullable = false)
    var externalId: String,

    @Column(name = "credential_ref", nullable = false)
    var credentialRef: UUID,

    @Column(nullable = false)
    var connectedByUserId: UUID,

    @Enumerated(EnumType.STRING)
    var status: StatusType = StatusType.PENDING,

    var lastSynced: Instant? = null
)