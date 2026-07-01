package dev.devault.authlib.service

import dev.devault.authlib.config.JwksClient
import dev.devault.authlib.config.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import java.util.Date
import java.util.UUID

class JwtClaimsService(
    private val jwksClient: JwksClient,
    private val jwtProperties: JwtProperties
) {

    fun validate(token: String) {
        val claims = try {
            extractAllClaims(token)
        } catch (_: ExpiredJwtException) {
            throw IllegalStateException("Token expired")
        } catch (_: JwtException) {
            throw IllegalStateException("Invalid token")
        }

        if (claims.issuer != jwtProperties.issuer) {
            throw IllegalStateException("Invalid issuer")
        }
    }
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(jwksClient.publicKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun<T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims: Claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    fun extractId(token: String): UUID = runCatching {
        UUID.fromString(extractClaim(token, Claims::getSubject))
    }.getOrElse { throw IllegalStateException("Invalid subject claim") }

    fun extractUsername(token: String): String =
        extractClaim(token) { it["username"] as? String
            ?: throw IllegalStateException("Invalid username claim") }

    fun extractJti(token: String): UUID = runCatching {
        extractClaim(token) { UUID.fromString(it["jti"].toString()) }
    }.getOrElse { throw IllegalStateException("Invalid jti claim") }

    fun extractExpiration(token: String): Date =
        extractClaim(token, Claims::getExpiration)

    fun extractAuthorities(token: String): List<String> =
        extractClaim(token) { claims ->
            val authorities = claims["authorities"]
            if (authorities is List<*>) {
                authorities.filterIsInstance<String>()
            } else {
                throw IllegalStateException("Invalid authorities claim")
            }
        }

}