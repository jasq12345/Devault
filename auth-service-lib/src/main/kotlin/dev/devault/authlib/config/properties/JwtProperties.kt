package dev.devault.authlib.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt.lib")
data class JwtProperties(
    val issuer: String
)
