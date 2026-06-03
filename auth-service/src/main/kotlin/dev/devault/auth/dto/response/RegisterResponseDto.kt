package dev.devault.auth.dto.response

import dev.devault.auth.type.RoleType
import java.util.UUID

data class RegisterResponseDto(
    val id: UUID,
    val email: String,
    val username: String,
    val role: MutableSet<RoleType>
)
