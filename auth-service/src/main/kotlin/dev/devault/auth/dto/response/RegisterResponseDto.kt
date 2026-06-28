package dev.devault.auth.dto.response

import dev.devault.auth.model.User
import dev.devault.auth.type.RoleType
import java.util.UUID

data class RegisterResponseDto(
    val id: UUID?,
    val email: String,
    val username: String,
    val authorities: MutableSet<RoleType>
)

fun User.toResponse() = RegisterResponseDto(
    id = id,
    email = email,
    username = username,
    authorities = authorities
)