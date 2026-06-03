package dev.devault.auth.dto.request

import jakarta.validation.constraints.NotBlank


data class LoginDto(
    @field:NotBlank(message = "Identifier must not be blank")
    val identifier: String,
    @field:NotBlank(message = "Password must not be blank")
    val password: String
)
