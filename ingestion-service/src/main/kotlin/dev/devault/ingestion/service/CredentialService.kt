package dev.devault.ingestion.service

import dev.devault.ingestion.model.Credential
import dev.devault.ingestion.repository.CredentialRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CredentialService(
    private val repository: CredentialRepository
) {

    fun save(token: String): UUID {
        val newCredential = repository.save(Credential(token = token))

        return newCredential.id!!
    }

    fun getToken(id: UUID): String {
        val credential = repository.findById(id)
            .orElseThrow{ NoSuchElementException("Credential not found") }

        return credential.token
    }
}