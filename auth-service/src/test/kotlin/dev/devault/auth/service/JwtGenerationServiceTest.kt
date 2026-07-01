package dev.devault.auth.service

import dev.devault.auth.config.properties.JwtProperties
import dev.devault.auth.security.provider.KeyProvider
import dev.devault.auth.security.principal.UserPrincipal
import io.jsonwebtoken.Jwts
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.util.UUID
import kotlin.math.abs

class JwtGenerationServiceTest {
    private val jwtProperties = mockk<JwtProperties>()
    private val keyProvider = mockk<KeyProvider>()

    private val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    @Nested
    inner class GenerateTokenPair {
        @Test
        fun `returns access and refresh tokens`() {
            every { keyProvider.getPrivateKey() } returns keyPair.private
            every { jwtProperties.issuer } returns "devault-auth"
            every { jwtProperties.accessExpiration } returns 900_000L
            every { jwtProperties.refreshExpiration } returns 604_800_000L

            val service = JwtGenerationService(jwtProperties, keyProvider)
            val userPrincipal = mockk<UserPrincipal>()
            every { userPrincipal.username } returns "testuser"
            every { userPrincipal.authorities } returns emptyList()
            every { userPrincipal.getId() } returns UUID.randomUUID()

            val result = service.generateTokenPair(userPrincipal)

            assert(result.accessToken.isNotBlank())
            assert(result.refreshToken.isNotBlank())
        }

        @Test
        fun `access token expires according to jwtProperties`() {
            every { keyProvider.getPrivateKey() } returns keyPair.private
            every { jwtProperties.issuer } returns "devault-auth"
            every { jwtProperties.accessExpiration } returns 900_000L
            every { jwtProperties.refreshExpiration } returns 604_800_000L

            val service = JwtGenerationService(jwtProperties, keyProvider)
            val userPrincipal = mockk<UserPrincipal>()
            every { userPrincipal.username } returns "testuser"
            every { userPrincipal.authorities } returns emptyList()
            every { userPrincipal.getId() } returns UUID.randomUUID()

            val beforeGeneration = System.currentTimeMillis()
            val result = service.generateTokenPair(userPrincipal)

            val accessClaims = Jwts.parser()
                .verifyWith(keyPair.public)
                .build()
                .parseSignedClaims(result.accessToken)
                .payload

            val expectedExpiration = beforeGeneration + 900_000L
            val actualExpiration = accessClaims.expiration.time

            assertTrue(abs(actualExpiration - expectedExpiration) < 5000)
        }
    }
}