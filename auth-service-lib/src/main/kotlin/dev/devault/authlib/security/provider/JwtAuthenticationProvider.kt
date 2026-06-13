package dev.devault.authlib.security.provider

import dev.devault.authlib.security.token.JwtTokenCandidate
import dev.devault.authlib.service.JwtClaimsService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationProvider(
    private val jwtClaimsService: JwtClaimsService
) : AuthenticationProvider{
    override fun authenticate(authentication: Authentication): Authentication? {
        val jwtTokenCandidate = authentication as? JwtTokenCandidate
            ?: return null
        val token = jwtTokenCandidate.credentials as? String
            ?: return null

        val username = jwtClaimsService.extractUsername(token)
        val authorities = jwtClaimsService.extractAuthorities(token)
            .map { SimpleGrantedAuthority(it) }

        return UsernamePasswordAuthenticationToken(username, null, authorities)
    }

    override fun supports(authentication: Class<*>): Boolean =
        JwtTokenCandidate::class.java.isAssignableFrom(authentication)
}