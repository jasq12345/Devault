package dev.devault.auth.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val privateKey: String,
    val publicKey: String,
    val accessExpiration: Long,
    val refreshExpiration: Long,
    val issuer: String
)