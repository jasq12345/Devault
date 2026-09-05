package dev.devault.ingestion.client.github.dto

import java.time.Instant

data class ActorAuthor(val login: String)

data class IssueLikeNode(
    val number: Int,
    val title: String,
    val body: String?,
    val url: String,
    val state: String,
    val createdAt: Instant,
    val author: ActorAuthor?
)

data class IssueLikeConnection(
    val pageInfo: PageInfo,
    val nodes: List<IssueLikeNode>
)