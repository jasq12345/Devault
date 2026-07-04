package dev.devault.authlib.security.provider

import dev.devault.authlib.service.JwtClaimsService
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JwtAuthenticationProviderTest {
    private val jwtClaimsService = mockk<JwtClaimsService>()
    private val provider = JwtAuthenticationProvider(jwtClaimsService)

    @Nested
    inner class Authenticate {

        @Test
        fun `returns authenticated token when jwt is valid`() {
        }

        @Test
        fun `returns null when authentication is not a JwtTokenCandidate`() {
        }

        @Test
        fun `returns null when credentials is not a string`() {
        }

        @Test
        fun `propagates exception when token validation fails`() {
        }
    }

    @Nested
    inner class Supports {

        @Test
        fun `returns true for JwtTokenCandidate`() {
        }

        @Test
        fun `returns false for unrelated authentication type`() {
        }
    }
}