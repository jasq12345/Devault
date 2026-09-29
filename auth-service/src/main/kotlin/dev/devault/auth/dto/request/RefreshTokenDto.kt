package dev.devault.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class RefreshTokenDto(
    @Schema(
        description = "Refresh token returned by /api/v1/auth/login",
        example = "eyJhbGciOiJFZERTQSJ9.eyJzdWIiOiJyZWZyZXNoIn0.c2lnbmF0dXJl"
    )
    val refreshToken: String
)
