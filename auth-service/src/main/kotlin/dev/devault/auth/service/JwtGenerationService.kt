package dev.devault.auth.service

import dev.devault.auth.security.principle.UserPrincipal
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey


@Service
class JwtGenerationService(
    @Value($$"${jwt.secret}")
    private val  secret: String,
    @Value($$"${jwt.expiration}")
    private val expiration: Long
){

    fun generateToken(principal: UserPrincipal): String {
        val claims: Map<String, Any> = hashMapOf(
            "username" to principal.username,
            "authorities" to principal.authorities.map { it.authority }
        )
        return createToken(claims, principal.getId())
    }

    private fun createToken(claims: Map<String, Any>, id: UUID): String {

        return Jwts.builder()
            .claims(claims)
            .subject(id.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(getSignKey())
            .compact()
    }
    private fun getSignKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    private fun extractAllClaims(token: String): Claims{
        return Jwts.parser()
            .verifyWith(getSignKey())
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

    fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractClaim(token, Claims::getExpiration).before(Date())
    }

    fun validateToken(token: String, principal: UserPrincipal): Boolean {
        val claims = extractAllClaims(token)
        val id = UUID.fromString(claims.subject)

        return id == principal.getId() && claims.expiration.after(Date())
    }
}