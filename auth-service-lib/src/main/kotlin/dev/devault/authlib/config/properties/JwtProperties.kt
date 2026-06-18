package dev.devault.authlib.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authlib.jwt")
data class JwtProperties(
    val issuer: String
)
