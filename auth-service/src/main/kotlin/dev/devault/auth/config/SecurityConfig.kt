package dev.devault.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
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
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
    ): AuthenticationProvider {
        return DaoAuthenticationProvider(userDetailsService)
    }

}