package dev.devault.ingestion.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.exception.ConflictException
import dev.devault.commonlib.type.WorkspaceRole
import dev.devault.ingestion.client.workspace.WorkspaceClient
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.exception.InsufficientPermissionException
import dev.devault.ingestion.exception.SourceAlreadyConnectedException
import dev.devault.ingestion.exception.SourceNotFoundException
import dev.devault.ingestion.exception.SourceSyncInProgressException
import dev.devault.ingestion.exception.WorkspaceServiceException
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class IngestionSourceServiceTest {
    private val repository = mockk<IngestionSourceRepository>()
    private val credentialService = mockk<CredentialService>()
    private val backfillService = mockk<BackfillService>()
    private val documentService = mockk<IngestedDocumentService>()
    private val workspaceClient = mockk<WorkspaceClient>()
    private val service = IngestionSourceService(repository, credentialService, backfillService, documentService, workspaceClient)

    private fun stubRole(workspaceId: UUID, role: WorkspaceRole) {
        every { workspaceClient.getWorkspaceMemberRole(workspaceId) } returns role
    }

    @Nested
    inner class ConnectSource {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val sourceId = UUID.randomUUID()
        private val dto = SaveIngestionSourceDto(owner = "octo", name = "repo", credentialRef = UUID.randomUUID())
        private val saved = slot<IngestionSource>()

        private fun stubHappyPath(role: WorkspaceRole = WorkspaceRole.OWNER) {
            stubRole(workspaceId, role)
            every { credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id) } returns "test-token-not-a-real-pat"
            every { repository.saveAndFlush(capture(saved)) } answers { saved.captured.apply { id = sourceId } }
            every { backfillService.runBackfill(sourceId) } returns Unit
        }

        private fun assertNothingHappened() { // nothing ever happens :)
            verify(exactly = 0) { credentialService.getTokenForUser(any(), any()) }
            verify(exactly = 0) { repository.saveAndFlush(any()) }
            verify(exactly = 0) { backfillService.runBackfill(any()) }
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

        @ParameterizedTest
        @EnumSource(WorkspaceRole::class, names = ["OWNER", "ADMIN"])
        fun `is allowed for owners and admins`(role: WorkspaceRole) {
            stubHappyPath(role)

            service.connectSource(authenticatedUser, workspaceId, dto)

            verify { repository.saveAndFlush(any()) }
            verify { backfillService.runBackfill(sourceId) }
        }

        // The role check needs the caller's token, which exists only on the request thread, not in the @Async backfill.
        @Test
        fun `checks the role, then credential ownership, then saves, then starts backfill`() {
            stubHappyPath()

            service.connectSource(authenticatedUser, workspaceId, dto)

            verifyOrder {
                workspaceClient.getWorkspaceMemberRole(workspaceId)
                credentialService.getTokenForUser(dto.credentialRef, authenticatedUser.id)
                repository.saveAndFlush(any())
                backfillService.runBackfill(sourceId)
            }
        }

        @Test
        fun `refuses a plain member with forbidden and touches nothing`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)

            val exception = assertThrows<InsufficientPermissionException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            assertIs<AccessDeniedException>(exception)
            assertNothingHappened()
        }

        @Test
        fun `refuses a non-member and touches nothing`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            assertNothingHappened()
        }

        @Test
        fun `throws not found and touches nothing when the workspace does not exist`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws NoSuchElementException("Workspace not found")

            assertThrows<NoSuchElementException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            assertNothingHappened()
        }

        @Test
        fun `touches nothing when workspace-service fails`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws
                WorkspaceServiceException("Workspace service request failed", RuntimeException())

            assertThrows<WorkspaceServiceException> {
                service.connectSource(authenticatedUser, workspaceId, dto)
            }

            assertNothingHappened()
        }

        @Test
        fun `response does not expose credentialRef`() {
            stubHappyPath()

            val result = service.connectSource(authenticatedUser, workspaceId, dto)

            assertFalse(result.toString().contains(dto.credentialRef.toString()))
        }

        @Test
        fun `does not save or start backfill when the credential belongs to another user`() {
            stubRole(workspaceId, WorkspaceRole.OWNER)
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
            stubRole(workspaceId, WorkspaceRole.OWNER)
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
            stubRole(workspaceId, WorkspaceRole.OWNER)
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
        private val callerId = UUID.randomUUID()
        private val otherMemberId = UUID.randomUUID()
        private val workspaceId = UUID.randomUUID()

        private fun source(externalId: String, status: StatusType, connectedBy: UUID = callerId) = IngestionSource(
            id = UUID.randomUUID(),
            workspaceId = workspaceId,
            externalId = externalId,
            credentialRef = UUID.randomUUID(),
            connectedByUserId = connectedBy,
            status = status
        )

        @Test
        fun `returns the workspace's sources in every status`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            val sources = listOf(
                source("octo/pending", StatusType.PENDING),
                source("octo/syncing", StatusType.SYNCING),
                source("octo/active", StatusType.ACTIVE),
                source("octo/error", StatusType.ERROR)
            )
            every { repository.findAllByWorkspaceId(workspaceId) } returns sources

            val result = service.findAllIngestedSources(workspaceId)

            assertEquals(sources.map { it.id }, result.map { it.id })
            assertEquals(listOf("octo/pending", "octo/syncing", "octo/active", "octo/error"), result.map { it.externalId })
            assertEquals(
                listOf(StatusType.PENDING, StatusType.SYNCING, StatusType.ACTIVE, StatusType.ERROR),
                result.map { it.status }
            )
            assertEquals(setOf(workspaceId), result.map { it.workspaceId }.toSet())
        }

        @Test
        fun `includes sources connected by other members`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            every { repository.findAllByWorkspaceId(workspaceId) } returns listOf(
                source("octo/mine", StatusType.ACTIVE, connectedBy = callerId),
                source("octo/theirs", StatusType.ACTIVE, connectedBy = otherMemberId)
            )

            val result = service.findAllIngestedSources(workspaceId)

            assertEquals(setOf(callerId, otherMemberId), result.map { it.connectedByUserId }.toSet())
        }

        @ParameterizedTest
        @EnumSource(WorkspaceRole::class)
        fun `is allowed for every role`(role: WorkspaceRole) {
            stubRole(workspaceId, role)
            every { repository.findAllByWorkspaceId(workspaceId) } returns emptyList()

            service.findAllIngestedSources(workspaceId)

            verify { repository.findAllByWorkspaceId(workspaceId) }
        }

        @Test
        fun `refuses a non-member without reading the sources`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> {
                service.findAllIngestedSources(workspaceId)
            }

            verify(exactly = 0) { repository.findAllByWorkspaceId(any()) }
        }

        @Test
        fun `throws not found without reading the sources when the workspace does not exist`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws NoSuchElementException("Workspace not found")

            assertThrows<NoSuchElementException> {
                service.findAllIngestedSources(workspaceId)
            }

            verify(exactly = 0) { repository.findAllByWorkspaceId(any()) }
        }

        @Test
        fun `response does not expose credentialRef`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            val source = source("octo/repo", StatusType.ACTIVE)
            every { repository.findAllByWorkspaceId(workspaceId) } returns listOf(source)

            val result = service.findAllIngestedSources(workspaceId)

            assertFalse(result.toString().contains(source.credentialRef.toString()))
        }

        @Test
        fun `returns an empty list when the workspace has no sources`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            every { repository.findAllByWorkspaceId(workspaceId) } returns emptyList()

            assertEquals(emptyList(), service.findAllIngestedSources(workspaceId))
        }
    }

    @Nested
    inner class FindIngestedSource {
        private val otherMemberId = UUID.randomUUID()
        private val workspaceId = UUID.randomUUID()
        private val sourceId = UUID.randomUUID()
        private val source = IngestionSource(
            id = sourceId,
            workspaceId = workspaceId,
            externalId = "octo/repo",
            credentialRef = UUID.randomUUID(),
            connectedByUserId = otherMemberId,
            status = StatusType.SYNCING
        )

        @Test
        fun `returns a source of the workspace, also one connected by another member`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            every { repository.findByIdAndWorkspaceId(sourceId, workspaceId) } returns source

            val result = service.findIngestedSource(workspaceId, sourceId)

            assertEquals(sourceId, result.id)
            assertEquals(workspaceId, result.workspaceId)
            assertEquals(SourceType.GITHUB, result.source)
            assertEquals("octo/repo", result.externalId)
            assertEquals(otherMemberId, result.connectedByUserId)
            assertEquals(StatusType.SYNCING, result.status)
        }

        @ParameterizedTest
        @EnumSource(WorkspaceRole::class)
        fun `is allowed for every role`(role: WorkspaceRole) {
            stubRole(workspaceId, role)
            every { repository.findByIdAndWorkspaceId(sourceId, workspaceId) } returns source

            assertEquals(sourceId, service.findIngestedSource(workspaceId, sourceId).id)
        }

        // A non-member gets 403 for any id, so the endpoint does not tell them which sources exist.
        @Test
        fun `refuses a non-member without looking the source up`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> {
                service.findIngestedSource(workspaceId, sourceId)
            }

            verify(exactly = 0) { repository.findByIdAndWorkspaceId(any(), any()) }
        }

        @Test
        fun `throws not found without looking the source up when the workspace does not exist`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws NoSuchElementException("Workspace not found")

            assertThrows<NoSuchElementException> {
                service.findIngestedSource(workspaceId, sourceId)
            }

            verify(exactly = 0) { repository.findByIdAndWorkspaceId(any(), any()) }
        }

        @Test
        fun `response does not expose credentialRef`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            every { repository.findByIdAndWorkspaceId(sourceId, workspaceId) } returns source

            val result = service.findIngestedSource(workspaceId, sourceId)

            assertFalse(result.toString().contains(source.credentialRef.toString()))
        }

        // The lookup is scoped to the workspace from the path, so a source of another workspace,
        // where the role was not checked, is not found.
        @Test
        fun `throws not found when there is no such source in the workspace`() {
            stubRole(workspaceId, WorkspaceRole.MEMBER)
            every { repository.findByIdAndWorkspaceId(sourceId, workspaceId) } returns null

            val exception = assertThrows<SourceNotFoundException> {
                service.findIngestedSource(workspaceId, sourceId)
            }

            // GlobalExceptionHandler maps NoSuchElementException to 404.
            assertIs<NoSuchElementException>(exception)
        }
    }

    @Nested
    inner class DeleteIngestedSource {
        private val otherMemberId = UUID.randomUUID()
        private val workspaceId = UUID.randomUUID()
        private val sourceId = UUID.randomUUID()

        private fun stubSource(status: StatusType, role: WorkspaceRole = WorkspaceRole.OWNER): IngestionSource {
            stubRole(workspaceId, role)
            val source = IngestionSource(
                id = sourceId,
                workspaceId = workspaceId,
                externalId = "octo/repo",
                credentialRef = UUID.randomUUID(),
                connectedByUserId = otherMemberId,
                status = status
            )
            every { repository.findByIdAndWorkspaceId(sourceId, workspaceId) } returns source
            every { documentService.deleteAllIngestedDocuments(source) } returns Unit
            every { repository.delete(source) } returns Unit
            return source
        }

        private fun assertNothingDeleted() {
            verify(exactly = 0) { documentService.deleteAllIngestedDocuments(any()) }
            verify(exactly = 0) { repository.delete(any()) }
        }

        @ParameterizedTest
        @EnumSource(StatusType::class, names = ["ACTIVE", "ERROR"])
        fun `deletes the documents first and then the source`(status: StatusType) {
            val source = stubSource(status)

            service.deleteIngestedSource(workspaceId, sourceId)

            verifyOrder {
                documentService.deleteAllIngestedDocuments(source)
                repository.delete(source)
            }
        }

        // The source in stubSource is connected by another member, so this also covers deleting someone else's source.
        @ParameterizedTest
        @EnumSource(WorkspaceRole::class, names = ["OWNER", "ADMIN"])
        fun `is allowed for owners and admins, also for a source connected by another member`(role: WorkspaceRole) {
            val source = stubSource(StatusType.ACTIVE, role)

            service.deleteIngestedSource(workspaceId, sourceId)

            verify { repository.delete(source) }
        }

        @Test
        fun `refuses a plain member with forbidden and deletes nothing`() {
            stubSource(StatusType.ACTIVE, WorkspaceRole.MEMBER)

            val exception = assertThrows<InsufficientPermissionException> {
                service.deleteIngestedSource(workspaceId, sourceId)
            }

            // GlobalExceptionHandler maps AccessDeniedException to 403.
            assertIs<AccessDeniedException>(exception)
            verify(exactly = 0) { repository.findByIdAndWorkspaceId(any(), any()) }
            assertNothingDeleted()
        }

        @Test
        fun `refuses a non-member and deletes nothing`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws AccessDeniedException("Access denied")

            assertThrows<AccessDeniedException> {
                service.deleteIngestedSource(workspaceId, sourceId)
            }

            verify(exactly = 0) { repository.findByIdAndWorkspaceId(any(), any()) }
            assertNothingDeleted()
        }

        @Test
        fun `throws not found and deletes nothing when the workspace does not exist`() {
            every { workspaceClient.getWorkspaceMemberRole(workspaceId) } throws NoSuchElementException("Workspace not found")

            assertThrows<NoSuchElementException> {
                service.deleteIngestedSource(workspaceId, sourceId)
            }

            assertNothingDeleted()
        }

        @ParameterizedTest
        @EnumSource(StatusType::class, names = ["PENDING", "SYNCING"])
        fun `refuses with a conflict while the backfill is waiting or running`(status: StatusType) {
            stubSource(status)

            val exception = assertThrows<SourceSyncInProgressException> {
                service.deleteIngestedSource(workspaceId, sourceId)
            }

            // GlobalExceptionHandler maps ConflictException to 409.
            assertIs<ConflictException>(exception)
            assertNothingDeleted()
        }

        @Test
        fun `throws not found and deletes nothing when there is no such source in the workspace`() {
            stubRole(workspaceId, WorkspaceRole.OWNER)
            every { repository.findByIdAndWorkspaceId(sourceId, workspaceId) } returns null

            assertThrows<SourceNotFoundException> {
                service.deleteIngestedSource(workspaceId, sourceId)
            }

            assertNothingDeleted()
        }

        // Mocks cannot show this: the bulk delete of documents needs a surrounding transaction, and
        // documents and source must disappear together or not at all.
        @Test
        fun `runs in one transaction`() {
            val method = IngestionSourceService::class.java.getMethod(
                "deleteIngestedSource",
                UUID::class.java,
                UUID::class.java
            )

            assertNotNull(method.getAnnotation(Transactional::class.java))
        }
    }

    @Nested
    inner class IsCredentialInUse {
        private val credentialRef = UUID.randomUUID()

        @Test
        fun `is true when a source references the credential`() {
            every { repository.existsByCredentialRef(credentialRef) } returns true

            assertEquals(true, service.isCredentialInUse(credentialRef))
        }

        @Test
        fun `is false when no source references the credential`() {
            every { repository.existsByCredentialRef(credentialRef) } returns false

            assertEquals(false, service.isCredentialInUse(credentialRef))
        }
    }
}
