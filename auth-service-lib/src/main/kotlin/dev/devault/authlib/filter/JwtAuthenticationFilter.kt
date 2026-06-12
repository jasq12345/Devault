package dev.devault.authlib.filter

import dev.devault.authlib.service.JwtClaimsService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtClaimsService: JwtClaimsService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization") ?: ""

        if (!authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        val identifier: String
        val authorities: List<SimpleGrantedAuthority>
        try {
            if (jwtClaimsService.isRefreshToken(token)) {
                filterChain.doFilter(request, response)
                return
            }
            identifier = jwtClaimsService.extractUsername(token)
            authorities = jwtClaimsService.extractAuthorities(token)
                .map { SimpleGrantedAuthority(it) }
        } catch (_: Exception) {
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val authToken = UsernamePasswordAuthenticationToken(identifier, null, authorities)

        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authToken

        filterChain.doFilter(request, response)
    }
}