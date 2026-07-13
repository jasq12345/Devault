package dev.devault.auth.service

import dev.devault.auth.security.provider.KeyProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.PublicKey
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwksServiceTest {
    private val keyProvider = mockk<KeyProvider>()
    private val publicKey = mockk<PublicKey>()

    @Nested
    inner class GetJwks {
        @Test
        fun `returns jwks with one key when public key is valid`() {
            every { keyProvider.getPublicKey() } returns publicKey
            every { publicKey.encoded } returns ByteArray(32) { it.toByte() }

            val service = JwksService(keyProvider)
            val result = service.getJwks()

            assertTrue(result.containsKey("keys"))
            val keys = result["keys"] as List<*>
            assertEquals(1, keys.size)
        }

        @Test
        fun `throws when public key encoding is too short`() {
            every { keyProvider.getPublicKey() } returns publicKey
            every { publicKey.encoded } returns ByteArray(10)

            assertThrows<IllegalArgumentException> {
                JwksService(keyProvider)
            }
        }
    }
}