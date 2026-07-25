package dev.devault.ingestion.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ingestion.security")
data class TokenEncryptionProperties(
    val key: String,
)
