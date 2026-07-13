package dev.devault.authlib.security.provider

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.authlib.security.token.JwtTokenCandidate
import dev.devault.authlib.service.JwtClaimsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID
import kotlin.test.assertEquals

class JwtAuthenticationProviderTest {
    private val jwtClaimsService = mockk<JwtClaimsService>()
    private val provider = JwtAuthenticationProvider(jwtClaimsService)

    @Nested
    inner class Authenticate {
        private val token = "token"

        @Test
        fun `returns authenticated token when jwt is valid`() {

            val candidate = mockk<JwtTokenCandidate>()
            val id = UUID.randomUUID()
            val username = "username"
            val authorities = listOf("USER", "ADMIN")
            val principal = AuthenticatedUser(id, username, authorities)

            every { candidate.credentials } returns token
            every { jwtClaimsService.validate(token) } returns Unit
            every { jwtClaimsService.extractId(token) } returns id
            every { jwtClaimsService.extractUsername(token) } returns username
            every { jwtClaimsService.extractAuthorities(token) } returns authorities

            val result = provider.authenticate(candidate)

            val authToken = assertInstanceOf<UsernamePasswordAuthenticationToken>(result)
            assertEquals(principal, authToken.principal)
            assertEquals(authorities.map { SimpleGrantedAuthority(it) }, authToken.authorities.toList())
        }

        @Test
        fun `returns null when authentication is not a JwtTokenCandidate`() {
            val authentication = mockk<Authentication>()

            val result = provider.authenticate(authentication)
            assertNull(result)
        }

        @Test
        fun `returns null when credentials is not a string`() {
            val candidate = mockk<JwtTokenCandidate>()

            every { candidate.credentials } returns Unit

            val result = provider.authenticate(candidate)
            assertNull(result)

        }

        @Test
        fun `propagates exception when token validation fails`() {
            val candidate = mockk<JwtTokenCandidate>()

            every { candidate.credentials } returns token
            every { jwtClaimsService.validate(token) } throws IllegalStateException("Token expired")

            assertThrows<IllegalStateException> {
                provider.authenticate(candidate)
            }
            verify(exactly = 0) { jwtClaimsService.extractId(any()) }
            verify(exactly = 0) { jwtClaimsService.extractUsername(any()) }
            verify(exactly = 0) { jwtClaimsService.extractAuthorities(any()) }
        }
    }

    @Nested
    inner class Supports {

        @Test
        fun `returns true for JwtTokenCandidate`() {
            val result = provider.supports(JwtTokenCandidate::class.java)

            assertTrue(result)
        }

        @Test
        fun `returns false for unrelated authentication type`() {
            val result = provider.supports(UsernamePasswordAuthenticationToken::class.java)

            assertFalse(result)
        }
    }
}