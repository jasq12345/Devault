package dev.devault.ingestion.config

import dev.devault.ingestion.config.properties.WorkspaceServiceProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.client.RestClient
import java.time.Clock

@EnableAsync
@EnableResilientMethods
@Configuration
@EnableConfigurationProperties(WorkspaceServiceProperties::class)
class IngestionAutoConfiguration {

    @Bean("githubRestClient")
    fun githubRestClient(): RestClient {
        return RestClient.create("https://api.github.com")
    }

    @Bean("workspaceRestClient")
    fun workspaceRestClient(properties: WorkspaceServiceProperties): RestClient {
        return RestClient.create(properties.uri)
    }

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}