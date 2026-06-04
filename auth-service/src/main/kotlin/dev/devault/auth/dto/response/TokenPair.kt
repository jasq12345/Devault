package dev.devault.auth.dto.response

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
