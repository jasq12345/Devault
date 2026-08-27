package dev.devault.ingestion.service

import dev.devault.ingestion.model.Credential
import dev.devault.ingestion.repository.CredentialRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CredentialService(
    private val repository: CredentialRepository
) {
    fun save(token: String, label: String, userId: UUID): UUID {
        val newCredential = repository.save(Credential(token = token, label = label, connectedByUserId = userId))
        return newCredential.id!!
    }

    fun findAllForUser(userId: UUID): List<Credential> {
        return repository.findAllByConnectedByUserId(userId)
    }

    fun getTokenForUser(id: UUID, userId: UUID): String {
        val credential = repository.findById(id)
            .orElseThrow { NoSuchElementException("Credential not found") }

        if (credential.connectedByUserId != userId) {
            throw AccessDeniedException("Access denied")
        }

        return credential.token
    }
}