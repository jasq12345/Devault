package dev.devault.auth.service

import dev.devault.auth.config.JwtProperties
import dev.devault.auth.config.KeyProvider
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.security.principal.UserPrincipal
import dev.devault.auth.type.TokenType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
    keyProvider: KeyProvider
){
    private val privateKey: PrivateKey = keyProvider.getPrivateKey()
    private val publicKey: PublicKey = keyProvider.getPublicKey()

    fun generateTokenPair(principal: UserPrincipal): TokenPair {
        val claims: Map<String, Any> = mapOf(
            "jti" to UUID.randomUUID(),
            "username" to principal.username,
            "authorities" to principal.authorities.map { it.authority },
            "type" to TokenType.ACCESS
        )
        val accessToken = createToken(claims, principal.getId(), TokenType.ACCESS)
        val refreshToken = createToken(mapOf("jti" to UUID.randomUUID(), "type" to TokenType.REFRESH ), principal.getId(), TokenType.REFRESH)

        return TokenPair(accessToken, refreshToken)
    }

    private fun createToken(claims: Map<String, Any>, id: UUID, access: TokenType): String {

        return Jwts.builder()
            .claims(claims)
            .subject(id.toString())
            .issuedAt(Date())
            .expiration(
                if(access == TokenType.ACCESS) Date(System.currentTimeMillis() + jwtProperties.accessExpiration)
                else Date(System.currentTimeMillis() + jwtProperties.refreshExpiration)
            )
            .signWith(privateKey)
            .compact()
    }

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
        extractClaim(token) { UUID.fromString(it["jti"] as String) }

    fun extractAuthorities(token: String): List<String> =
        extractClaim(token) {
            @Suppress("UNCHECKED_CAST")
            it["authorities"] as List<String>
        }

    fun extractExpiration(token: String): Date =
        extractClaim(token, Claims::getExpiration)

    fun validateToken(token: String, principal: UserPrincipal): Boolean {
        val claims = extractAllClaims(token)
        val id = UUID.fromString(claims.subject)

        return id == principal.getId() && claims.expiration.after(Date())
    }

    fun isRefreshToken(token: String): Boolean =
        extractClaim(token) { it["type"] } == TokenType.REFRESH
}