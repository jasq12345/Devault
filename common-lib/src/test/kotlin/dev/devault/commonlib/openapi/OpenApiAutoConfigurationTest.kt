package dev.devault.commonlib.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class OpenApiAutoConfigurationTest {
    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration::class.java))
        .withPropertyValues("spring.application.name=test-service")

    @Test
    fun `registers OpenAPI with application name as title and global bearer JWT scheme`() {
        contextRunner.run { context ->
            val openApi = context.getBean(OpenAPI::class.java)
            val scheme = openApi.components.securitySchemes[OpenApiAutoConfiguration.BEARER_SCHEME]

            assertEquals("test-service", openApi.info.title)
            assertEquals(SecurityScheme.Type.HTTP, scheme?.type)
            assertEquals("bearer", scheme?.scheme)
            assertEquals("JWT", scheme?.bearerFormat)
            assertTrue(openApi.security.any { it.containsKey(OpenApiAutoConfiguration.BEARER_SCHEME) })
        }
    }

    @Test
    fun `backs off when springdoc is not on the classpath`() {
        contextRunner
            .withClassLoader(FilteredClassLoader("org.springdoc"))
            .run { context -> assertTrue(context.getBeansOfType(OpenAPI::class.java).isEmpty()) }
    }

    @Test
    fun `backs off when api docs are disabled`() {
        contextRunner
            .withPropertyValues("springdoc.api-docs.enabled=false")
            .run { context -> assertTrue(context.getBeansOfType(OpenAPI::class.java).isEmpty()) }
    }

    @Test
    fun `keeps OpenAPI bean defined by the service`() {
        contextRunner
            .withUserConfiguration(CustomOpenApiConfig::class.java)
            .run { context -> assertEquals("custom", context.getBean(OpenAPI::class.java).info.title) }
    }

    @Configuration(proxyBeanMethods = false)
    class CustomOpenApiConfig {
        @Bean
        fun customOpenApi(): OpenAPI = OpenAPI().info(Info().title("custom").version("1"))
    }
}
