package dev.devault.ingestion.security.converter

import dev.devault.ingestion.config.properties.TokenEncryptionProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class TokenAttributeConverterTest {
    private val converter = TokenAttributeConverter(TokenEncryptionProperties(randomKey()))
    private val token = "test-token-not-a-real-pat"

    @Test
    fun `decrypting an encrypted token returns the original`() {
        val stored = converter.convertToDatabaseColumn(token)

        assertEquals(token, converter.convertToEntityAttribute(stored))
    }

    @Test
    fun `token with non-ASCII characters survives the round trip`() {
        val unicodeToken = "zażółć-gęślą-jaźń-🔑"

        val stored = converter.convertToDatabaseColumn(unicodeToken)

        assertEquals(unicodeToken, converter.convertToEntityAttribute(stored))
    }

    @Test
    fun `stored value does not contain the plain token`() {
        val stored = converter.convertToDatabaseColumn(token)!!
        val storedBytes = String(Base64.getDecoder().decode(stored), Charsets.ISO_8859_1)

        assertFalse(stored.contains(token))
        assertFalse(storedBytes.contains(token))
    }

    @Test
    fun `stored value is IV, ciphertext and GCM tag`() {
        val stored = converter.convertToDatabaseColumn(token)!!

        val expectedSize = 12 + token.toByteArray(Charsets.UTF_8).size + 16
        assertEquals(expectedSize, Base64.getDecoder().decode(stored).size)
    }

    @Test
    fun `encrypting the same token twice gives different values`() {
        val first = converter.convertToDatabaseColumn(token)
        val second = converter.convertToDatabaseColumn(token)

        assertNotEquals(first, second)
    }

    @Test
    fun `null stays null in both directions`() {
        assertNull(converter.convertToDatabaseColumn(null))
        assertNull(converter.convertToEntityAttribute(null))
    }

    @Test
    fun `tampered value is rejected`() {
        val bytes = Base64.getDecoder().decode(converter.convertToDatabaseColumn(token))
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 1).toByte()
        val tampered = Base64.getEncoder().encodeToString(bytes)

        assertThrows<AEADBadTagException> {
            converter.convertToEntityAttribute(tampered)
        }
    }

    @Test
    fun `value encrypted with another key is rejected`() {
        val otherConverter = TokenAttributeConverter(TokenEncryptionProperties(randomKey()))
        val stored = otherConverter.convertToDatabaseColumn(token)

        assertThrows<AEADBadTagException> {
            converter.convertToEntityAttribute(stored)
        }
    }

    private fun randomKey(): String =
        Base64.getEncoder().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
}
