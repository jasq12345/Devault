package dev.devault.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val privateKey: String,
    val publicKey: String,
    val accessExpiration: Long,
    val refreshExpiration: Long
)