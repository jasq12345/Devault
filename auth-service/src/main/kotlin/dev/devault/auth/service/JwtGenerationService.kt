package dev.devault.auth.service

import dev.devault.auth.config.properties.JwtProperties
import dev.devault.auth.security.provider.KeyProvider
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.security.principal.UserPrincipal
import dev.devault.authlib.type.TokenType
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import java.security.PrivateKey
import java.util.Date
import java.util.UUID

@Service
class JwtGenerationService(
    private val jwtProperties: JwtProperties,
    keyProvider: KeyProvider
){
    private val privateKey: PrivateKey = keyProvider.getPrivateKey()

    fun generateTokenPair(principal: UserPrincipal): TokenPair {
        val claims: Map<String, Any> = mapOf(
            "jti" to UUID.randomUUID().toString(),
            "username" to principal.username,
            "authorities" to principal.authorities.map { it.authority },
            "type" to TokenType.ACCESS.name.lowercase()
        )
        val accessToken = createToken(claims, principal.getId(), TokenType.ACCESS)
        val refreshToken = createToken(mapOf("jti" to UUID.randomUUID().toString(), "type" to TokenType.REFRESH.name.lowercase() ), principal.getId(), TokenType.REFRESH)

        return TokenPair(accessToken, refreshToken)
    }

    private fun createToken(claims: Map<String, Any>, id: UUID, access: TokenType): String {

        return Jwts.builder()
            .claims(claims)
            .subject(id.toString())
            .issuer(jwtProperties.issuer)
            .issuedAt(Date())
            .expiration(
                if(access == TokenType.ACCESS) Date(System.currentTimeMillis() + jwtProperties.accessExpiration)
                else Date(System.currentTimeMillis() + jwtProperties.refreshExpiration)
            )
            .signWith(privateKey)
            .compact()
    }
}