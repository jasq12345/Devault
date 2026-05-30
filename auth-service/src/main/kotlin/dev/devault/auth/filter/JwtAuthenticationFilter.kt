package dev.devault.auth.filter

import dev.devault.auth.security.principle.UserPrincipal
import dev.devault.auth.service.JwtService
import dev.devault.auth.service.UserDetailsServiceImpl
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsServiceImpl
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader: String = request.getHeader("Authorization") ?: ""
        var token: String? = null
        var id: UUID? = null
        var username: String? = null

        if(authHeader != "" && authHeader.startsWith("Bearer ")){
            token = authHeader.substring(7)
            id = jwtService.extractId(token)
            username = jwtService.extractUserName(token)
        }

        if(id != null && username != null && token != null && SecurityContextHolder.getContext().authentication == null){
            val principal: UserPrincipal = userDetailsService.loadUserByUsername(username)
            if(jwtService.validateToken(token, principal)){
                val authToken = UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.authorities
                )

                authToken.details =
                    WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authToken
            }
        }
        filterChain.doFilter(request, response)
    }
}