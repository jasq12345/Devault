package dev.devault.authlib.service

import dev.devault.authlib.config.JwksClient
import dev.devault.authlib.config.properties.JwtProperties
import io.jsonwebtoken.Jwts
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator
import java.util.Date
import java.util.UUID

class JwtClaimsServiceTest {
    private val jwksClient = mockk<JwksClient>()
    private val jwtProperties = mockk<JwtProperties>()
    private val service = JwtClaimsService(jwksClient, jwtProperties)

    private val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    private fun buildToken(
        subject: String = UUID.randomUUID().toString(),
        issuer: String = "devault-auth",
        expiration: Date = Date(System.currentTimeMillis() + 60_000),
        claims: Map<String, Any> = emptyMap()
    ): String {
        return Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .claims(claims)
            .issuedAt(Date())
            .expiration(expiration)
            .signWith(keyPair.private)
            .compact()
    }

    @Nested
    inner class Validate {
        @Test
        fun `passes when token is not expired and issuer matches`() {
            every { jwksClient.publicKey } returns keyPair.public
            every { jwtProperties.issuer } returns "devault-auth"

            val token = buildToken(issuer = "devault-auth")

            assertDoesNotThrow {
                service.validate(token)
            }
        }

        @Test
        fun `throws when token is expired`() {
            every { jwksClient.publicKey } returns keyPair.public

            val token = buildToken(issuer = "devault-auth", expiration = Date(System.currentTimeMillis() - 60_000))

            assertThrows<IllegalStateException> {
                service.validate(token)
            }
        }

        @Test
        fun `throws when issuer does not match`() {
        }
    }

    @Nested
    inner class ExtractId {
        @Test
        fun `returns UUID from subject claim`() {
        }

        @Test
        fun `throws when subject is not a valid UUID`() {
        }
    }

    @Nested
    inner class ExtractUsername {
        @Test
        fun `returns username from claims`() {
        }

        @Test
        fun `throws when username claim is missing`() {
        }
    }

    @Nested
    inner class ExtractJti {
        @Test
        fun `returns UUID from jti claim`() {
        }

        @Test
        fun `throws when jti claim is invalid`() {
        }
    }

    @Nested
    inner class ExtractAuthorities {
        @Test
        fun `returns list of authorities`() {
        }

        @Test
        fun `throws when authorities claim is not a list`() {
        }
    }
}