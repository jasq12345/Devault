package dev.devault.commonlib.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.customizers.GlobalOperationCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * Shared OpenAPI setup for every service that has springdoc on the classpath
 * (added by the `devault.springdoc` Gradle convention plugin).
 */
@AutoConfiguration(beforeName = ["org.springdoc.core.configuration.SpringDocConfiguration"])
@ConditionalOnClass(name = ["org.springdoc.core.configuration.SpringDocConfiguration"])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = ["springdoc.api-docs.enabled"], matchIfMissing = true)
class OpenApiAutoConfiguration {

    init {
        // The principal (e.g. AuthenticatedUser) comes from the JWT, it is not a client-supplied parameter
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal::class.java)
    }

    @Bean
    @ConditionalOnMissingBean
    fun openApi(
        @Value("\${spring.application.name:api}") applicationName: String,
        @Value("\${spring.application.version:v0}") applicationVersion: String,
    ): OpenAPI = OpenAPI()
        .info(Info().title(applicationName).version(applicationVersion))
        .components(Components().addSecuritySchemes(BEARER_SCHEME, bearerJwtScheme()))
        .addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    /**
     * Readable names for API clients (e.g. Insomnia folders and request names):
     * tag `workspace-member-controller` -> `workspace-member`, summary from method `findWorkspaceById` -> `find workspace by id`.
     * Explicit `@Tag` / `@Operation(summary = ...)` on a controller take precedence.
     */
    @Bean
    fun operationNamingCustomizer(): GlobalOperationCustomizer = GlobalOperationCustomizer { operation, handlerMethod ->
        operation.tags = operation.tags?.map { it.removeSuffix(CONTROLLER_TAG_SUFFIX) }
        if (operation.summary.isNullOrBlank()) {
            operation.summary = handlerMethod.method.name.replace(CAMEL_CASE_BOUNDARY, "$1 $2").lowercase()
        }
        operation
    }

    /**
     * Declares every operation tag at the top level of the spec - importers such as Insomnia
     * create folders only from top-level tags.
     */
    @Bean
    fun topLevelTagsCustomizer(): GlobalOpenApiCustomizer = GlobalOpenApiCustomizer { openApi ->
        val declaredTags = openApi.tags.orEmpty().map { it.name }.toSet()
        openApi.paths.orEmpty().values
            .asSequence()
            .flatMap { it.readOperations() }
            .flatMap { it.tags.orEmpty() }
            .distinct()
            .filterNot { it in declaredTags }
            .sorted()
            .toList()
            .forEach { openApi.addTagsItem(Tag().name(it)) }
    }

    private fun bearerJwtScheme(): SecurityScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")

    companion object {
        const val BEARER_SCHEME = "bearerAuth"
        private const val CONTROLLER_TAG_SUFFIX = "-controller"
        private val CAMEL_CASE_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    }
}
