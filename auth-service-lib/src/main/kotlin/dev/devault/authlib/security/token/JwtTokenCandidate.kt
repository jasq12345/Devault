package dev.devault.authlib.security.token

import org.springframework.security.authentication.AbstractAuthenticationToken

class JwtTokenCandidate(private val token: String) : AbstractAuthenticationToken(null) {
    override fun getCredentials(): Any = token
    override fun getPrincipal(): Any? = null
    override fun isAuthenticated(): Boolean = false
}