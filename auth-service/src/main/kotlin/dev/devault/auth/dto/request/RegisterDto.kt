package dev.devault.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class RegisterDto(
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Username must not be blank")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username can only contain letters, numbers, and underscores"
    )
    val username: String,
    @field:NotBlank(message = "Password must not be blank")
    val password: String
)