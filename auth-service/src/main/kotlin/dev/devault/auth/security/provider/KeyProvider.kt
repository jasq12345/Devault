package dev.devault.auth.security.provider

import dev.devault.auth.config.properties.JwtProperties
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Component
class KeyProvider(
    jwtProperties: JwtProperties
) {
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

    internal fun getPrivateKey(): PrivateKey = privateKey
    fun getPublicKey(): PublicKey = publicKey
}