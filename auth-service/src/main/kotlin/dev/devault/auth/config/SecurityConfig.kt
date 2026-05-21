package dev.devault.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { customizer -> customizer.disable() }
            .authorizeHttpRequests {
                it.apply{
                    requestMatchers("/auth/**").permitAll()
                    anyRequest().authenticated()
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic { }
            .build()
    }

    @Bean
    fun loadUserFromUsername(): UserDetails? {
        return null
    }
}