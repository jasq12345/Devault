package dev.devault.ingestion.service

import dev.devault.commonlib.exception.ConflictException
import dev.devault.ingestion.exception.CredentialInUseException
import dev.devault.ingestion.exception.CredentialNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CredentialDeletionServiceTest {
    private val credentialService = mockk<CredentialService>()
    private val sourceService = mockk<IngestionSourceService>()
    private val service = CredentialDeletionService(credentialService, sourceService)

    private val userId = UUID.randomUUID()
    private val credentialId = UUID.randomUUID()

    @Test
    fun `checks ownership, then usage, then deletes`() {
        every { credentialService.requireOwned(credentialId, userId) } returns Unit
        every { sourceService.isCredentialInUse(credentialId) } returns false
        every { credentialService.delete(credentialId, userId) } returns Unit

        service.delete(credentialId, userId)

        verifyOrder {
            credentialService.requireOwned(credentialId, userId)
            sourceService.isCredentialInUse(credentialId)
            credentialService.delete(credentialId, userId)
        }
    }

    @Test
    fun `refuses with a conflict while a source uses the credential`() {
        every { credentialService.requireOwned(credentialId, userId) } returns Unit
        every { sourceService.isCredentialInUse(credentialId) } returns true

        val exception = assertThrows<CredentialInUseException> {
            service.delete(credentialId, userId)
        }

        // GlobalExceptionHandler maps ConflictException to 409.
        assertIs<ConflictException>(exception)
        verify(exactly = 0) { credentialService.delete(any(), any()) }
    }

    @Test
    fun `answers not found for a missing or foreign credential without revealing whether it is in use`() {
        every { credentialService.requireOwned(credentialId, userId) } throws
            CredentialNotFoundException("Credential not found")

        assertThrows<CredentialNotFoundException> {
            service.delete(credentialId, userId)
        }

        verify(exactly = 0) { sourceService.isCredentialInUse(any()) }
        verify(exactly = 0) { credentialService.delete(any(), any()) }
    }

    // Mocks cannot show this: the ownership check, the usage check and the delete have to share one transaction.
    @Test
    fun `runs in one transaction`() {
        val method = CredentialDeletionService::class.java.getMethod("delete", UUID::class.java, UUID::class.java)

        assertNotNull(method.getAnnotation(Transactional::class.java))
    }
}
