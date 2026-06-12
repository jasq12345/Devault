package dev.devault.authlib.config

import com.nimbusds.jose.jwk.JWKSet
import dev.devault.authlib.config.properties.JwksProperties
import jakarta.annotation.PostConstruct
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import java.security.PublicKey

class JwksClient(
    private val jwksProperties: JwksProperties,
    private val restTemplate: RestTemplate
) {
    private lateinit var publicKey: PublicKey

    @PostConstruct
    fun init() {
        val jwksResponse = restTemplate.getForObject<String>(jwksProperties.uri)
            ?: throw IllegalStateException("Could not fetch JWKS")
        val jwkSet = JWKSet.parse(jwksResponse)
        publicKey = jwkSet.keys[0].toOctetKeyPair().toPublicKey()
    }

    fun getPublicKey(): PublicKey = publicKey
}