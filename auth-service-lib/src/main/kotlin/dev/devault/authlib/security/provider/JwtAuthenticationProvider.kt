package dev.devault.authlib.security.provider

import dev.devault.authlib.security.principal.AuthenticatedUser
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

        jwtClaimsService.validate(token)

        val id = jwtClaimsService.extractId(token)
        val username = jwtClaimsService.extractUsername(token)
        val authoritiesList = jwtClaimsService.extractAuthorities(token)
        val authorities = authoritiesList.map { SimpleGrantedAuthority(it) }

        val principal = AuthenticatedUser(id, username, authoritiesList)
        return UsernamePasswordAuthenticationToken(principal, null, authorities)
    }

    override fun supports(authentication: Class<*>): Boolean =
        JwtTokenCandidate::class.java.isAssignableFrom(authentication)
}