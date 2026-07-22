package dev.devault.authlib.config

import com.nimbusds.jose.jwk.JWKSet
import dev.devault.authlib.config.properties.JwksProperties
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class JwksClient(
    private val jwksProperties: JwksProperties,
    private val restClient: RestClient,
) {
    @Volatile
    private var cachedSet: JWKSet? = null
    @Volatile
    private var cachedAt: Instant = Instant.MIN
    private val refreshLock = ReentrantLock()

    fun getPublicKey(): PublicKey {
        val okp = findKey() ?: run {
            refresh(force = true)
            findKey() ?: throw IllegalStateException("No key found in JWKS")
        }
        return toPublicKey(okp)
    }

    private fun findKey(): com.nimbusds.jose.jwk.OctetKeyPair? {
        ensureFresh()
        return cachedSet?.keys?.firstOrNull()?.toOctetKeyPair()
    }

    private fun ensureFresh() {
        val expired = Instant.now().isAfter(cachedAt.plusSeconds(jwksProperties.cacheTtlSeconds))
        if (cachedSet == null || expired) {
            refresh(force = false)
        }
    }

    private fun refresh(force: Boolean) {
        refreshLock.withLock {
            val expired = Instant.now().isAfter(cachedAt.plusSeconds(jwksProperties.cacheTtlSeconds))
            if (!force && cachedSet != null && !expired) return
            try {
                val jwksResponse = restClient.get()
                    .uri(jwksProperties.uri)
                    .retrieve()
                    .body<String>()
                    ?: throw IllegalStateException("Could not fetch JWKS")
                cachedSet = JWKSet.parse(jwksResponse)
                cachedAt = Instant.now()
            } catch (e: ResourceAccessException) {
                throw IllegalStateException("Auth service unavailable", e)
            }
        }
    }

    private fun toPublicKey(okp: com.nimbusds.jose.jwk.OctetKeyPair): PublicKey {
        val rawBytes = okp.x.decode()
        val encoded = prependAsn1Header(rawBytes)
        return KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(encoded))
    }

    private fun prependAsn1Header(rawKey: ByteArray): ByteArray {
        val header = byteArrayOf(0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00)
        return header + rawKey
    }
}