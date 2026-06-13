package dev.devault.authlib.config

import dev.devault.authlib.config.properties.JwksProperties
import dev.devault.authlib.filter.JwtAuthenticationFilter
import dev.devault.authlib.security.provider.JwtAuthenticationProvider
import dev.devault.authlib.service.JwtClaimsService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(JwksProperties::class)
class AuthLibAutoConfiguration {

    @Bean
    fun restClient(): RestClient = RestClient.create()

    @Bean
    fun jwtAuthenticationFilter(jwtAuthenticationProvider: JwtAuthenticationProvider): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtAuthenticationProvider)

    @Bean
    fun jwtClaimsService(jwksClient: JwksClient): JwtClaimsService =
        JwtClaimsService(jwksClient)

    @Bean
    @Lazy
    fun jwksClient(jwksProperties: JwksProperties, restClient: RestClient): JwksClient =
        JwksClient(jwksProperties, restClient)

    @Bean
    fun jwtAuthenticationProvider(jwtClaimsService: JwtClaimsService): JwtAuthenticationProvider =
        JwtAuthenticationProvider(jwtClaimsService)
}