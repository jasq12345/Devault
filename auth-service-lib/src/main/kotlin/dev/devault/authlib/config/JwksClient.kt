package dev.devault.authlib.config

import com.nimbusds.jose.jwk.JWKSet
import dev.devault.authlib.config.properties.JwksProperties
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class JwksClient(
    private val jwksProperties: JwksProperties,
    private val restClient: RestClient
) {
    private val publicKey: PublicKey by lazy { fetchPublicKey() }

    fun getPublicKey(): PublicKey = publicKey

    private fun fetchPublicKey(): PublicKey {
        try {
            val jwksResponse = restClient.get()
                .uri(jwksProperties.uri)
                .retrieve()
                .body<String>()
                ?: throw IllegalStateException("Could not fetch JWKS")

            val jwkSet = JWKSet.parse(jwksResponse)
            val okp = jwkSet.keys.firstOrNull()?.toOctetKeyPair()
                ?: throw IllegalStateException("No Octet Key Pair found in JWKS")

            val rawBytes = okp.x.decode()
            val encoded = prependAsn1Header(rawBytes)
            return KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(encoded))
        } catch (e: ResourceAccessException) {
            throw IllegalStateException("Auth service unavailable", e)
        }
    }

    // ASN.1 DER header for Ed25519 public key (RFC 8410)
    private fun prependAsn1Header(rawKey: ByteArray): ByteArray {
        val header = byteArrayOf(0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00)
        return header + rawKey
    }
}