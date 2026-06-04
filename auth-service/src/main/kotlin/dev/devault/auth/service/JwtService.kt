package dev.devault.auth.service

import dev.devault.auth.config.JwtProperties
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.security.principal.UserPrincipal
import dev.devault.auth.type.TokenType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    private val jwtProperties: JwtProperties
){
    private val privateKey: PrivateKey
    private val publicKey: PublicKey

    init {
        val keyFactory = KeyFactory.getInstance("Ed25519")
        privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(Base64.getDecoder().decode(jwtProperties.privateKey))
        )
        publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(jwtProperties.publicKey))
        )
    }

    fun generateTokenPair(principal: UserPrincipal): TokenPair {
        val claims: Map<String, Any> = mapOf(
            "username" to principal.username,
            "authorities" to principal.authorities.map { it.authority },
            "type" to TokenType.ACCESS.name.lowercase()
        )
        val accessToken = createToken(claims, principal.getId(), TokenType.ACCESS)
        val refreshToken = createToken(mapOf("type" to TokenType.REFRESH.name.lowercase() ), principal.getId(), TokenType.REFRESH)

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

    private fun extractAllClaims(token: String): Claims{
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

    fun extractId(token: String): UUID {
        val subject = extractClaim(token, Claims::getSubject)
        return UUID.fromString(subject)
    }

    fun extractUsername(token: String): String {
        return extractClaim(token) { it["username"] as String }
    }

    fun validateToken(token: String, principal: UserPrincipal): Boolean {
        val claims = extractAllClaims(token)
        val id = UUID.fromString(claims.subject)

        return id == principal.getId() && claims.expiration.after(Date())
    }

    fun isRefreshToken(token: String): Boolean =
        extractClaim(token) { it["type"] } == TokenType.REFRESH.name.lowercase()
}