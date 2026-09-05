package dev.devault.ingestion.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.client.RestClient

@EnableAsync
@Configuration
class IngestionAutoConfiguration {

    @Bean("gitHubRestClient")
    fun gitHubRestClient(): RestClient {
        return RestClient.create("https://api.github.com")
    }
}