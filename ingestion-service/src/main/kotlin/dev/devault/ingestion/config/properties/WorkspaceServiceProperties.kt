package dev.devault.ingestion.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "workspace.service")
data class WorkspaceServiceProperties(
    val uri: String
)
