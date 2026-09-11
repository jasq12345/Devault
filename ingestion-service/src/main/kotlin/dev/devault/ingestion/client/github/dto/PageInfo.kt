package dev.devault.ingestion.client.github.dto

data class PageInfo(
    val hasNextPage: Boolean,
    val endCursor: String? = null
)
