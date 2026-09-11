package dev.devault.ingestion.model

import dev.devault.ingestion.security.converter.TokenAttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "credentials")
class Credential(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var label: String,

    @Column(nullable = false)
    var connectedByUserId: UUID,

    @Convert(converter = TokenAttributeConverter::class)
    @Column(nullable = false)
    var token: String
)