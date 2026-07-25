package dev.devault.ingestion.security.converter

import dev.devault.ingestion.config.properties.TokenEncryptionProperties
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
@Converter
class TokenAttributeConverter(
    properties: TokenEncryptionProperties
) : AttributeConverter<String, String> {

    private val key = SecretKeySpec(Base64.getDecoder().decode(properties.key), "AES")

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null

        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        val ciphertext = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))

        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null

        val decoded = Base64.getDecoder().decode(dbData)
        val iv = decoded.copyOfRange(0, 12)
        val ciphertext = decoded.copyOfRange(12, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}