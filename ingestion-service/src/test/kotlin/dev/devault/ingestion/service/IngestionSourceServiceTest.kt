package dev.devault.ingestion.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.exception.ConflictException
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.exception.SourceAlreadyConnectedException
import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestionSourceRepository
import dev.devault.ingestion.type.SourceType
import dev.devault.ingestion.type.StatusType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class IngestionSourceServiceTest {
    private val repository = mockk<IngestionSourceRepository>()
    private val credentialService = mockk<CredentialService>()
    private val backfillService = mockk<BackfillService>()
    private val service = IngestionSourceService(repository, credentialService, backfillService)

    @Nested
    inner class ConnectSource {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val sourceId = UUID.randomUUID()
        private val dto = SaveIngestionSourceDto(owner = "octo", name = "repo", credentialRef = UUID.randomUUID())
        private val saved = slot<IngestionSource>()

        private fun stubHappyPath() {
            every { credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id) } returns "test-token-not-a-real-pat"
            every { repository.saveAndFlush(capture(saved)) } answers { saved.captured.apply { id = sourceId } }
            every { backfillService.runBackfill(sourceId) } returns Unit
        }

        @Test
        fun `saves a pending GitHub source for the workspace and caller`() {
            stubHappyPath()

            val result = service.connectSource(authenticatedUser, workspaceId, dto)

            assertEquals(workspaceId, saved.captured.workspaceId)
            assertEquals("octo/repo", saved.captured.externalId)
            assertEquals(dto.credentialRef, saved.captured.credentialRef)
            assertEquals(authenticatedUser.id, saved.captured.connectedByUserId)

            assertEquals(sourceId, result.id)
            assertEquals(workspaceId, result.workspaceId)
            assertEquals(SourceType.GITHUB, result.source)
            assertEquals("octo/repo", result.externalId)
            assertEquals(authenticatedUser.id, result.connectedByUserId)
            assertEquals(StatusType.PENDING, result.status)
        }

        @Test
        fun `checks credential ownership, then saves, then starts backfill`() {
            stubHappyPath()

            service.connectSource(authenticatedUser, workspaceId, dto)

            verifyOrder {
                credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id)
                repository.saveAndFlush(any())
                backfillService.runBackfill(sourceId)
            }
        }

        @Test
        fun `response does not expose credentialRef`() {
            stubHappyPath()

            val result = service.connectSource(authenticatedUser, workspaceId, dto)

            assertFalse(result.toString().contains(dto.credentialRef.toString()))
        }

        @Test
        fun `does not save or start backfill when the credential belongs to another user`() {
            every { credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id) } throws
                AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            verify(exactly = 0) { repository.saveAndFlush(any()) }
            verify(exactly = 0) { backfillService.runBackfill(any()) }
        }

        @Test
        fun `throws conflict and does not start backfill when the source is already connected`() {
            every { credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id) } returns "test-token-not-a-real-pat"
            every { repository.saveAndFlush(any()) } throws DataIntegrityViolationException("duplicate key")

            val exception = assertThrows<SourceAlreadyConnectedException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            // GlobalExceptionHandler maps ConflictException to 409.
            assertIs<ConflictException>(exception)
            verify(exactly = 0) { backfillService.runBackfill(any()) }
        }

        @Test
        fun `stores externalId in lowercase so the same repository cannot be connected twice`() {
            val mixedCaseDto = SaveIngestionSourceDto(owner = "Octo-Org", name = "My.Repo", credentialRef = dto.credentialRef)
            stubHappyPath()

            val result = service.connectSource(authenticatedUser, workspaceId, mixedCaseDto)

            assertEquals("octo-org/my.repo", saved.captured.externalId)
            assertEquals("octo-org/my.repo", result.externalId)
        }

        @Test
        fun `does not save or start backfill when the credential does not exist`() {
            every { credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id) } throws
                NoSuchElementException("Credential not found")

            assertThrows<NoSuchElementException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            verify(exactly = 0) { repository.saveAndFlush(any()) }
            verify(exactly = 0) { backfillService.runBackfill(any()) }
        }
    }

    @Nested
    inner class FindAllIngestedSources {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()

        private fun source(externalId: String, status: StatusType) = IngestionSource(
            id = UUID.randomUUID(),
            workspaceId = workspaceId,
            externalId = externalId,
            credentialRef = UUID.randomUUID(),
            connectedByUserId = authenticatedUser.id,
            status = status
        )

        @Test
        fun `returns the caller's sources of the workspace in every status`() {
            val sources = listOf(
                source("octo/pending", StatusType.PENDING),
                source("octo/syncing", StatusType.SYNCING),
                source("octo/active", StatusType.ACTIVE),
                source("octo/error", StatusType.ERROR)
            )
            every { repository.findAllByConnectedByUserIdAndWorkspaceId(authenticatedUser.id, workspaceId) } returns sources

            val result = service.findAllIngestedSources(authenticatedUser, workspaceId)

            assertEquals(sources.map { it.id }, result.map { it.id })
            assertEquals(listOf("octo/pending", "octo/syncing", "octo/active", "octo/error"), result.map { it.externalId })
            assertEquals(
                listOf(StatusType.PENDING, StatusType.SYNCING, StatusType.ACTIVE, StatusType.ERROR),
                result.map { it.status }
            )
            assertEquals(setOf(workspaceId), result.map { it.workspaceId }.toSet())
            assertEquals(setOf(authenticatedUser.id), result.map { it.connectedByUserId }.toSet())
        }

        @Test
        fun `response does not expose credentialRef`() {
            val source = source("octo/repo", StatusType.ACTIVE)
            every { repository.findAllByConnectedByUserIdAndWorkspaceId(authenticatedUser.id, workspaceId) } returns listOf(source)

            val result = service.findAllIngestedSources(authenticatedUser, workspaceId)

            assertFalse(result.toString().contains(source.credentialRef.toString()))
        }

        @Test
        fun `returns an empty list when the caller has no sources in the workspace`() {
            every { repository.findAllByConnectedByUserIdAndWorkspaceId(authenticatedUser.id, workspaceId) } returns emptyList()

            assertEquals(emptyList(), service.findAllIngestedSources(authenticatedUser, workspaceId))
        }
    }
}
