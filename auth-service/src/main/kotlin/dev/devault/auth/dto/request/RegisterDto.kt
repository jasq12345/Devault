package dev.devault.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class RegisterDto(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Schema(example = "jan.kowalski@example.com")
    val email: String,

    @NotBlank(message = "Username must not be blank")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username can only contain letters, numbers, and underscores"
    )
    @Schema(example = "jan_kowalski")
    val username: String,
    @NotBlank(message = "Password must not be blank")
    @Schema(example = "Secret123!")
    val password: String
)