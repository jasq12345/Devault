package dev.devault.authlib.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authlib.jwks")
data class JwksProperties(
    val uri: String
)