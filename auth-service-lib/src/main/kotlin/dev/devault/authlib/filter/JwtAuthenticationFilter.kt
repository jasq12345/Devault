package dev.devault.authlib.filter

import dev.devault.authlib.security.entrypoint.AuthLibAuthenticationEntryPoint
import dev.devault.authlib.security.provider.JwtAuthenticationProvider
import dev.devault.authlib.security.token.JwtTokenCandidate
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtAuthenticationProvider,
    private val entryPoint: AuthLibAuthenticationEntryPoint
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
            ?: return filterChain.doFilter(request, response)
        if (!authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }
        val token = authHeader.removePrefix("Bearer ")
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }
        val authToken: Authentication
        try {
            val candidate = JwtTokenCandidate(token)
            authToken = jwtProvider.authenticate(candidate)
                ?: run {
                    filterChain.doFilter(request, response)
                    return
                }
        } catch (ex: Exception) {
            SecurityContextHolder.clearContext()
            entryPoint.commence(request, response, BadCredentialsException("Invalid or expired token", ex))
            return
        }
        SecurityContextHolder.getContext().authentication = authToken
        filterChain.doFilter(request, response)
    }
}