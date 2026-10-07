package dev.devault.ingestion.service

import dev.devault.ingestion.dto.request.CredentialRequestDto
import dev.devault.ingestion.dto.response.CredentialResponseDto
import dev.devault.ingestion.dto.response.toResponse
import dev.devault.ingestion.exception.CredentialNotFoundException
import dev.devault.ingestion.model.Credential
import dev.devault.ingestion.repository.CredentialRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CredentialService(
    private val repository: CredentialRepository
) {
    fun save(dto: CredentialRequestDto, userId: UUID): CredentialResponseDto {
        val newCredential = repository.save(Credential(token = dto.token, label = dto.label, connectedByUserId = userId))
        return newCredential.toResponse()
    }

    fun findAllForUser(userId: UUID): List<CredentialResponseDto> {
        val credentials = repository.findAllByConnectedByUserId(userId)

        return credentials.map { it.toResponse() }.toList()
    }

    fun getTokenForUser(id: UUID, userId: UUID): String {
        val credential = repository.findById(id)
            .orElseThrow { NoSuchElementException("Credential not found") }

        if (credential.connectedByUserId != userId) {
            throw AccessDeniedException("Access denied")
        }

        return credential.token
    }

    fun requireOwned(id: UUID, userId: UUID) {
        findOwned(id, userId)
    }

    // Does not check whether a source still uses the credential; that is CredentialDeletionService's job.
    fun delete(id: UUID, userId: UUID) {
        repository.delete(findOwned(id, userId))
    }

    private fun findOwned(id: UUID, userId: UUID): Credential {
        return repository.findByIdAndConnectedByUserId(id, userId)
            ?: throw CredentialNotFoundException("Credential not found")
    }
}