package dev.devault.authlib.service

import dev.devault.authlib.config.JwksProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import java.security.PublicKey
import java.util.Date
import java.util.UUID

class JwtClaimsService(
    jwksProperties: JwksProperties
) {

    private val publicKey: PublicKey = jwksProperties.uri
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun<T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims: Claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    fun extractId(token: String): UUID =
        UUID.fromString(extractClaim(token, Claims::getSubject))

    fun extractUsername(token: String): String =
        extractClaim(token) { it["username"] as String }

    fun extractJti(token: String): UUID =
        extractClaim(token) { UUID.fromString(it["jti"].toString()) }

    fun extractExpiration(token: String): Date =
        extractClaim(token, Claims::getExpiration)

}