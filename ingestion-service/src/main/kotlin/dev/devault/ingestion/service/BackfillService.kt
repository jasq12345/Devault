package dev.devault.ingestion.service

import dev.devault.ingestion.client.github.GitHubClient
import dev.devault.ingestion.client.github.dto.CommitNode
import dev.devault.ingestion.client.github.dto.IssueLikeNode
import dev.devault.ingestion.model.IngestedDocument
import dev.devault.ingestion.model.IngestionSource
import dev.devault.ingestion.repository.IngestedDocumentRepository
import dev.devault.ingestion.repository.IngestionSourceRepository
import dev.devault.ingestion.type.DocumentType
import dev.devault.ingestion.type.StatusType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class BackfillService(
    private val documentRepository: IngestedDocumentRepository,
    private val sourceRepository: IngestionSourceRepository,
    private val gitHubClient: GitHubClient
) {

    private val IngestionSource.githubOwner: String
        get() = externalId.substringBefore("/")

    private val IngestionSource.githubName: String
        get() = externalId.substringAfter("/")

    @Async
    fun runBackfill(sourceId: UUID) {
        val source = sourceRepository.findById(sourceId)
            .orElseThrow { NoSuchElementException("Ingestion source not found: $sourceId") }
        source.status = StatusType.SYNCING
        sourceRepository.save(source)

        try {
            syncCommits(source)
            syncPullRequests(source)
            syncIssues(source)
            source.status = StatusType.ACTIVE
        } catch (_: Exception) {
            source.status = StatusType.ERROR
        } finally {
            sourceRepository.save(source)
        }
    }

    private fun syncCommits(source: IngestionSource) {
        var cursor: String? = null
        do {
            val page = gitHubClient.fetchCommitHistory(source.githubOwner, source.githubName, source.credentialRef, cursor)
            page.nodes.forEach { node -> saveAsIngestedDocument(source, node) }
            cursor = page.pageInfo.endCursor
        } while (page.pageInfo.hasNextPage)
    }

    private fun syncPullRequests(source: IngestionSource) {
        var cursor: String? = null
        do {
            val page = gitHubClient.fetchPullRequests(source.githubOwner, source.githubName, source.credentialRef, cursor)
            page.nodes.forEach { node -> saveAsIngestedDocument(source, node, DocumentType.PULL_REQUEST) }
            cursor = page.pageInfo.endCursor
        } while (page.pageInfo.hasNextPage)
    }

    private fun syncIssues(source: IngestionSource) {
        var cursor: String? = null
        do {
            val page = gitHubClient.fetchIssues(source.githubOwner, source.githubName, source.credentialRef, cursor)
            page.nodes.forEach { node -> saveAsIngestedDocument(source, node, DocumentType.ISSUE) }
            cursor = page.pageInfo.endCursor
        } while (page.pageInfo.hasNextPage)
    }

    private fun saveAsIngestedDocument(source: IngestionSource, node: CommitNode) {
        save(source, externalRef = node.oid, content = node.message, type = DocumentType.COMMIT)
    }

    private fun saveAsIngestedDocument(source: IngestionSource, node: IssueLikeNode, type: DocumentType) {
        save(source, externalRef = node.number.toString(), content = "${node.title}\n\n${node.body ?: ""}", type = type)
    }

    private fun save(source: IngestionSource, externalRef: String, content: String, type: DocumentType) {
        val doc = IngestedDocument(
            source = source,
            externalRef = externalRef,
            contentHash = sha256(content),
            rawContent = content,
            documentType = type,
            ingestedAt = Instant.now()
        )
        try {
            documentRepository.saveAndFlush(doc)
        } catch (_: DataIntegrityViolationException) { }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}