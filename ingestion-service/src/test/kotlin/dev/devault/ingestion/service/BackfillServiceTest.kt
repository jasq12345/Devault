package dev.devault.ingestion.service

import dev.devault.ingestion.client.github.GitHubClient
import dev.devault.ingestion.client.github.dto.CommitNode
import dev.devault.ingestion.client.github.dto.HistoryConnection
import dev.devault.ingestion.client.github.dto.IssueLikeConnection
import dev.devault.ingestion.client.github.dto.IssueLikeNode
import dev.devault.ingestion.client.github.dto.PageInfo
import dev.devault.ingestion.exception.GitHubApiException
import dev.devault.ingestion.model.IngestedDocument
import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestedDocumentRepository
import dev.devault.ingestion.repository.IngestionSourceRepository
import dev.devault.ingestion.type.DocumentType
import dev.devault.ingestion.type.StatusType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BackfillServiceTest {
    private val documentRepository = mockk<IngestedDocumentRepository>()
    private val sourceRepository = mockk<IngestionSourceRepository>()
    private val gitHubClient = mockk<GitHubClient>()
    private val service = BackfillService(documentRepository, sourceRepository, gitHubClient)

    private val sourceId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val credentialRef = UUID.randomUUID()
    private val source = IngestionSource(
        id = sourceId,
        workspaceId = UUID.randomUUID(),
        externalId = "octo/repo",
        credentialRef = credentialRef,
        connectedByUserId = userId,
        status = StatusType.PENDING
    )

    // The same entity instance is saved twice, so the status has to be recorded at the time of each save.
    private val savedStatuses = mutableListOf<StatusType>()
    private val savedDocuments = mutableListOf<IngestedDocument>()

    @BeforeEach
    fun stubEmptyRepository() {
        every { sourceRepository.findById(sourceId) } returns Optional.of(source)
        every { sourceRepository.save(any()) } answers {
            firstArg<IngestionSource>().also { savedStatuses += it.status }
        }
        every { documentRepository.saveAndFlush(capture(savedDocuments)) } answers { firstArg() }
        every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) } returns commitPage()
        every { gitHubClient.fetchPullRequests(any(), any(), any(), any(), any()) } returns issueLikePage()
        every { gitHubClient.fetchIssues(any(), any(), any(), any(), any()) } returns issueLikePage()
    }

    @Nested
    inner class Status {
        @Test
        fun `marks the source as SYNCING first and ACTIVE when finished`() {
            service.runBackfill(sourceId)

            assertEquals(listOf(StatusType.SYNCING, StatusType.ACTIVE), savedStatuses)
        }

        @Test
        fun `marks the source as ERROR when GitHub fails and stops syncing`() {
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) } throws
                GitHubApiException("boom")

            service.runBackfill(sourceId)

            assertEquals(listOf(StatusType.SYNCING, StatusType.ERROR), savedStatuses)
            verify(exactly = 0) { gitHubClient.fetchPullRequests(any(), any(), any(), any(), any()) }
            verify(exactly = 0) { gitHubClient.fetchIssues(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `marks the source as ERROR when a later step fails`() {
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) } returns commitPage(commit("abc"))
            every { gitHubClient.fetchIssues(any(), any(), any(), any(), any()) } throws IllegalStateException("boom")

            service.runBackfill(sourceId)

            assertEquals(listOf(StatusType.SYNCING, StatusType.ERROR), savedStatuses)
            assertEquals(listOf("abc"), savedDocuments.map { it.externalRef })
        }

        @Test
        fun `throws and saves nothing when the source does not exist`() {
            val unknownId = UUID.randomUUID()
            every { sourceRepository.findById(unknownId) } returns Optional.empty()

            assertThrows<NoSuchElementException> {
                service.runBackfill(unknownId)
            }

            verify(exactly = 0) { sourceRepository.save(any()) }
            verify(exactly = 0) { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class GitHubCalls {
        @Test
        fun `uses owner and name from externalId with the user and credential of the source`() {
            service.runBackfill(sourceId)

            verifyOrder {
                gitHubClient.fetchCommitHistory(userId, "octo", "repo", credentialRef, null)
                gitHubClient.fetchPullRequests(userId, "octo", "repo", credentialRef, null)
                gitHubClient.fetchIssues(userId, "octo", "repo", credentialRef, null)
            }
        }

        @Test
        fun `follows the commit cursor until there is no next page`() {
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), null) } returns
                commitPage(commit("a1"), commit("a2"), nextCursor = "cursor-1")
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), "cursor-1") } returns
                commitPage(commit("b1"), nextCursor = "cursor-2")
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), "cursor-2") } returns
                commitPage(commit("c1"))

            service.runBackfill(sourceId)

            verify(exactly = 3) { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) }
            assertEquals(listOf("a1", "a2", "b1", "c1"), savedDocuments.map { it.externalRef })
        }

        @Test
        fun `follows the pull request and issue cursors independently`() {
            every { gitHubClient.fetchPullRequests(any(), any(), any(), any(), null) } returns
                issueLikePage(issueLike(1), nextCursor = "pr-cursor")
            every { gitHubClient.fetchPullRequests(any(), any(), any(), any(), "pr-cursor") } returns
                issueLikePage(issueLike(2))
            every { gitHubClient.fetchIssues(any(), any(), any(), any(), null) } returns
                issueLikePage(issueLike(3), nextCursor = "issue-cursor")
            every { gitHubClient.fetchIssues(any(), any(), any(), any(), "issue-cursor") } returns
                issueLikePage(issueLike(4))

            service.runBackfill(sourceId)

            assertEquals(
                listOf("1" to DocumentType.PULL_REQUEST, "2" to DocumentType.PULL_REQUEST, "3" to DocumentType.ISSUE, "4" to DocumentType.ISSUE),
                savedDocuments.map { it.externalRef to it.documentType }
            )
        }
    }

    @Nested
    inner class Documents {
        @Test
        fun `stores a commit with oid as externalRef and message as content`() {
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) } returns
                commitPage(commit("abc123", message = "feat: add backfill"))

            service.runBackfill(sourceId)

            val document = savedDocuments.single()
            assertSame(source, document.source)
            assertEquals("abc123", document.externalRef)
            assertEquals("feat: add backfill", document.rawContent)
            assertEquals(DocumentType.COMMIT, document.documentType)
            assertTrue(document.ingestedAt != null)
        }

        @Test
        fun `stores a pull request with number as externalRef and title plus body as content`() {
            every { gitHubClient.fetchPullRequests(any(), any(), any(), any(), any()) } returns
                issueLikePage(issueLike(42, title = "Add backfill", body = "Closes #7"))

            service.runBackfill(sourceId)

            val document = savedDocuments.single()
            assertEquals("42", document.externalRef)
            assertEquals("Add backfill\n\nCloses #7", document.rawContent)
            assertEquals(DocumentType.PULL_REQUEST, document.documentType)
        }

        @Test
        fun `stores an issue with number as externalRef and title plus body as content`() {
            every { gitHubClient.fetchIssues(any(), any(), any(), any(), any()) } returns
                issueLikePage(issueLike(7, title = "Backfill is slow", body = "Takes minutes"))

            service.runBackfill(sourceId)

            val document = savedDocuments.single()
            assertEquals("7", document.externalRef)
            assertEquals("Backfill is slow\n\nTakes minutes", document.rawContent)
            assertEquals(DocumentType.ISSUE, document.documentType)
        }

        @Test
        fun `missing body is stored as title only`() {
            every { gitHubClient.fetchIssues(any(), any(), any(), any(), any()) } returns
                issueLikePage(issueLike(7, title = "No description", body = null))

            service.runBackfill(sourceId)

            assertEquals("No description\n\n", savedDocuments.single().rawContent)
        }

        @Test
        fun `content hash is the lowercase hex SHA-256 of the content`() {
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) } returns
                commitPage(commit("abc123", message = "abc"))

            service.runBackfill(sourceId)

            // Known SHA-256 test vector for "abc" (FIPS 180-2).
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                savedDocuments.single().contentHash
            )
        }

        @Test
        fun `skips a duplicate document and keeps going`() {
            every { gitHubClient.fetchCommitHistory(any(), any(), any(), any(), any()) } returns
                commitPage(commit("first"), commit("duplicate"), commit("last"))
            every { documentRepository.saveAndFlush(match { it.externalRef == "duplicate" }) } throws
                DataIntegrityViolationException("duplicate key")

            service.runBackfill(sourceId)

            assertEquals(listOf("first", "last"), savedDocuments.map { it.externalRef })
            assertEquals(listOf(StatusType.SYNCING, StatusType.ACTIVE), savedStatuses)
            verify(exactly = 1) { gitHubClient.fetchPullRequests(any(), any(), any(), any(), any()) }
        }
    }

    private fun commit(oid: String, message: String = "message of $oid") =
        CommitNode(oid = oid, message = message, committedDate = Instant.EPOCH, author = null)

    private fun commitPage(vararg nodes: CommitNode, nextCursor: String? = null) =
        HistoryConnection(PageInfo(hasNextPage = nextCursor != null, endCursor = nextCursor), nodes.toList())

    private fun issueLike(number: Int, title: String = "title $number", body: String? = "body $number") =
        IssueLikeNode(
            number = number,
            title = title,
            body = body,
            url = "https://github.com/octo/repo/issues/$number",
            state = "OPEN",
            createdAt = Instant.EPOCH,
            author = null
        )

    private fun issueLikePage(vararg nodes: IssueLikeNode, nextCursor: String? = null) =
        IssueLikeConnection(PageInfo(hasNextPage = nextCursor != null, endCursor = nextCursor), nodes.toList())
}
