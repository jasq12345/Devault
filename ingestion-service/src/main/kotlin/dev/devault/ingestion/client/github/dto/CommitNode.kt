package dev.devault.ingestion.client.github.dto

import java.time.Instant

data class CommitNode(
    val oid: String,
    val message: String,
    val committedDate: Instant,
    val author: CommitAuthor?
)