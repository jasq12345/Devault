package dev.devault.authlib.security.principal

import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val username: String,
    val authorities: List<String>
)