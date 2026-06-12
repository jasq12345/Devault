package dev.devault.authlib.config

import dev.devault.authlib.config.properties.JwksProperties
import dev.devault.authlib.filter.JwtAuthenticationFilter
import dev.devault.authlib.service.JwtClaimsService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(JwksProperties::class)
class AuthLibAutoConfiguration {

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    @Bean
    fun jwtAuthenticationFilter(jwtClaimsService: JwtClaimsService): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtClaimsService)

    @Bean
    fun jwtClaimsService(jwksClient: JwksClient): JwtClaimsService =
        JwtClaimsService(jwksClient)

    @Bean
    fun jwksClient(jwksProperties: JwksProperties, restTemplate: RestTemplate): JwksClient =
        JwksClient(jwksProperties, restTemplate)
}