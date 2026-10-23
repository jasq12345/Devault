package dev.devault.authlib.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authlib.security")
data class AuthLibSecurityProperties(
    // Paths reachable without a JWT (e.g. OpenAPI docs), relative to the servlet context path
    val publicPaths: List<String> = emptyList(),
)
