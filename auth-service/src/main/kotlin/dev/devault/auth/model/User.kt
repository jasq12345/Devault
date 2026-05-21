package dev.devault.auth.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class User(
    @Id
    val id: UUID = UUID.randomUUID(),
    val username: String,
    val email: String,
    val roles: List<String> = listOf(),
    val enabled: Boolean = true,
    val password: String? = null,
)
