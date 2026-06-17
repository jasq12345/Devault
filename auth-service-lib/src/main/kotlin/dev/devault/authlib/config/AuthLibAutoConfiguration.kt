package dev.devault.authlib.config

import dev.devault.authlib.config.properties.JwksProperties
import dev.devault.authlib.config.properties.JwtProperties
import dev.devault.authlib.filter.JwtAuthenticationFilter
import dev.devault.authlib.security.provider.JwtAuthenticationProvider
import dev.devault.authlib.service.JwtClaimsService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(JwksProperties::class, JwtProperties::class)
class AuthLibAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    fun securityFilterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun restClient(): RestClient = RestClient.create()

    @Bean
    fun jwtAuthenticationFilter(jwtAuthenticationProvider: JwtAuthenticationProvider): JwtAuthenticationFilter =
        JwtAuthenticationFilter(jwtAuthenticationProvider)

    @Bean
    fun jwtClaimsService(jwksClient: JwksClient, jwtProperties: JwtProperties): JwtClaimsService =
        JwtClaimsService(jwksClient, jwtProperties)

    @Bean
    @Lazy
    fun jwksClient(jwksProperties: JwksProperties, restClient: RestClient): JwksClient =
        JwksClient(jwksProperties, restClient)

    @Bean
    fun jwtAuthenticationProvider(jwtClaimsService: JwtClaimsService): JwtAuthenticationProvider =
        JwtAuthenticationProvider(jwtClaimsService)
}