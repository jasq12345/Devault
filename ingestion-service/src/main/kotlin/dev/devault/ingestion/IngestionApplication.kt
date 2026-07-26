package dev.devault.ingestion

import dev.devault.ingestion.config.properties.TokenEncryptionProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(TokenEncryptionProperties::class)

class IngestionApplication

fun main(args: Array<String>) {
    runApplication<IngestionApplication>(*args)
}
