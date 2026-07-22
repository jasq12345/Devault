package dev.devault.authlib.service

import dev.devault.authlib.config.JwksClient
import dev.devault.authlib.config.properties.JwtProperties
import io.jsonwebtoken.Jwts
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator
import java.util.Date
import java.util.UUID
import kotlin.IllegalStateException

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
            every { jwksClient.getPublicKey() } returns keyPair.public
            every { jwtProperties.issuer } returns "devault-auth"

            val token = buildToken(issuer = "devault-auth")

            assertDoesNotThrow {
                service.validate(token)
            }
        }

        @Test
        fun `throws when token is expired`() {
            every { jwksClient.getPublicKey() } returns keyPair.public

            val token = buildToken(issuer = "devault-auth", expiration = Date(System.currentTimeMillis() - 60_000))

            assertThrows<IllegalStateException> {
                service.validate(token)
            }
        }

        @Test
        fun `throws when issuer does not match`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            every { jwtProperties.issuer } returns "devault-auth"

            val token = buildToken(issuer = "not-devault-auth")

            assertThrows<IllegalStateException> {
                service.validate(token)
            }
        }
    }

    @Nested
    inner class ExtractId {

        @Test
        fun `returns UUID from subject claim`() {
            val id = UUID.randomUUID()
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(subject = id.toString())

            val result = service.extractId(token)

            assertEquals(id, result)
        }

        @Test
        fun `throws when subject is not a valid UUID`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(subject = "not-UUID")

            assertThrows<IllegalStateException> {
                service.extractId(token)
            }
        }
    }

    @Nested
    inner class ExtractUsername {
        @Test
        fun `returns username from claims`() {
            val username = "username"
            every { jwksClient.getPublicKey() } returns keyPair.public

            val token = buildToken(claims = mapOf("username" to username))

            val result = service.extractUsername(token)
            assertEquals(username, result)
        }

        @Test
        fun `throws when username claim is missing`() {
            every { jwksClient.getPublicKey() } returns keyPair.public

            val token = buildToken()

            assertThrows<IllegalStateException> {
                service.extractUsername(token)
            }
        }
    }

    @Nested
    inner class ExtractJti {
        @Test
        fun `returns UUID from jti claim`() {
            val jti = UUID.randomUUID()
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(claims = mapOf("jti" to jti.toString()))

            val result = service.extractJti(token)

            assertEquals(jti, result)
        }

        @Test
        fun `throws when jti claim is invalid`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(claims = mapOf("jti" to "not-jti"))

            assertThrows<IllegalStateException> {
                service.extractJti(token)
            }
        }
    }

    @Nested
    inner class ExtractExpiration {
        @Test
        fun `returns expiration date from claims`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            val expiration = Date(System.currentTimeMillis() + 120_000)
            val token = buildToken(expiration = expiration)

            val result = service.extractExpiration(token)

            assertEquals(expiration.time / 1000, result.time / 1000)
        }

        @Test
        fun `throws when token is expired`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(expiration = Date(System.currentTimeMillis() - 60_000))

            assertThrows<IllegalStateException> {
                service.extractExpiration(token)
            }
        }
    }

    @Nested
    inner class ExtractAuthorities {
        @Test
        fun `returns list of authorities`() {
            val authorities = listOf("USER")
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(claims = mapOf("authorities" to listOf("USER")))

            val result = service.extractAuthorities(token)

            assertEquals(authorities, result)
        }

        @Test
        fun `throws when authorities claim is not a list`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(claims = mapOf("authorities" to "USER"))

            assertThrows<IllegalStateException> {
                service.extractAuthorities(token)
            }
        }

        @Test
        fun `throws when authorities list contains non-string elements`() {
            every { jwksClient.getPublicKey() } returns keyPair.public
            val token = buildToken(claims = mapOf("authorities" to listOf("USER", 123, "ADMIN")))

            assertThrows<IllegalStateException> {
                service.extractAuthorities(token)
            }
        }
    }
}