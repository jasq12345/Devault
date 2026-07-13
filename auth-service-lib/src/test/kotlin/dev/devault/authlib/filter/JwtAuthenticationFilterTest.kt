package dev.devault.authlib.filter

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.authlib.security.provider.JwtAuthenticationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class JwtAuthenticationFilterTest {
    private val jwtProvider = mockk<JwtAuthenticationProvider>(relaxed = true)
    private val filter = JwtAuthenticationFilter(jwtProvider)

    private val request = mockk<HttpServletRequest>(relaxed = true)
    private val response = mockk<HttpServletResponse>(relaxed = true)
    private val filterChain = mockk<FilterChain>(relaxed = true)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        every { request.getAttribute(any()) } returns null
        every { request.dispatcherType } returns DispatcherType.REQUEST
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Nested
    inner class DoFilterInternal {

        @Test
        fun `sets authentication in context when token is valid`() {
            val authHeader = "Bearer bearer-token"
            val authenticatedToken = UsernamePasswordAuthenticationToken(
                AuthenticatedUser(UUID.randomUUID(), "testuser", listOf("USER")),
                null,
                listOf(SimpleGrantedAuthority("USER"))
            )

            every { request.getHeader("Authorization") } returns authHeader
            every { jwtProvider.authenticate(any()) } returns authenticatedToken
            every { filterChain.doFilter(request, response) } returns Unit

            filter.doFilter(request, response, filterChain)

            verify(exactly = 1) { filterChain.doFilter(request, response) }
            verify (exactly = 1) { jwtProvider.authenticate(any()) }
            assertEquals(authenticatedToken, SecurityContextHolder.getContext().authentication)
        }

        @Test
        fun `passes through when authorization header is missing`() {
            every { request.getHeader("Authorization") } returns null
            every { filterChain.doFilter(request, response) } returns Unit

            filter.doFilter(request, response, filterChain)

            verify (exactly = 1) { filterChain.doFilter(request, response) }
            verify (exactly = 0) { jwtProvider.authenticate(any()) }
        }

        @Test
        fun `passes through when authorization header does not start with Bearer`() {
            val authHeader = "not-a-bearer-token"
            every { request.getHeader("Authorization") } returns authHeader
            every { filterChain.doFilter(request, response) } returns Unit

            filter.doFilter(request, response, filterChain)

            verify (exactly = 1) { filterChain.doFilter(request, response) }
            verify (exactly = 0) { jwtProvider.authenticate(any()) }
        }

        @Test
        fun `passes through when context already has authentication`() {
            val existingAuth = mockk<Authentication>()
            SecurityContextHolder.getContext().authentication = existingAuth

            val authHeader = "Bearer bearer-token"
            every { request.getHeader("Authorization") } returns authHeader
            every { filterChain.doFilter(request, response) } returns Unit

            filter.doFilter(request, response, filterChain)

            verify(exactly = 1) { filterChain.doFilter(request, response) }
            verify(exactly = 0) { jwtProvider.authenticate(any()) }
        }

        @Test
        fun `passes through without setting context when provider returns null`() {
            val authHeader = "Bearer bearer-token"
            every { request.getHeader("Authorization") } returns authHeader
            every { jwtProvider.authenticate(any()) } returns null
            every { filterChain.doFilter(request, response) } returns Unit

            filter.doFilter(request, response, filterChain)

            verify(exactly = 1) { filterChain.doFilter(request, response) }
            verify (exactly = 1) { jwtProvider.authenticate(any()) }
            assertNull(SecurityContextHolder.getContext().authentication)
        }

        @Test
        fun `clears context and passes through when provider throws`() {
            val authHeader = "Bearer bearer-token"
            every { request.getHeader("Authorization") } returns authHeader
            every { jwtProvider.authenticate(any()) } throws IllegalStateException("Invalid token")
            every { filterChain.doFilter(request, response) } returns Unit

            filter.doFilter(request, response, filterChain)

            verify(exactly = 1) { filterChain.doFilter(request, response) }
            verify(exactly = 1) { jwtProvider.authenticate(any()) }
            assertNull(SecurityContextHolder.getContext().authentication)
        }
    }
}