package dev.devault.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank


data class LoginDto(
    @NotBlank(message = "Identifier must not be blank")
    @Schema(description = "Username or email", example = "jan_kowalski")
    val identifier: String,
    @NotBlank(message = "Password must not be blank")
    @Schema(example = "Secret123!")
    val password: String
)
