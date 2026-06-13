package dev.devault.auth.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import dev.devault.auth.security.provider.KeyProvider
import org.springframework.stereotype.Service
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL

@Service
class JwksService(
    keyProvider: KeyProvider
) {
    private val publicKey = keyProvider.getPublicKey()
    private val jwks: Map<String, Any> = buildJwks()

    fun getJwks(): Map<String, Any> = jwks

    private fun buildJwks() : Map<String, Any> {
        val encoded = publicKey.encoded
        require(encoded.size >= 32) {
            "Unexpected public key encoding length: ${encoded.size}"
        }

        val keyBytes = encoded.takeLast(32).toByteArray()

        val octetKeyPair = OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(keyBytes))
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.EdDSA)
            .build()

        return mapOf("keys" to listOf(octetKeyPair.toJSONObject()))
    }
}