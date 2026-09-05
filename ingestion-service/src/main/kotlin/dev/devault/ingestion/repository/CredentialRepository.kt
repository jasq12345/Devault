package dev.devault.ingestion.repository

import dev.devault.ingestion.model.Credential
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CredentialRepository : JpaRepository<Credential, UUID> {
    fun findAllByConnectedByUserId(userId: UUID): List<Credential>
}