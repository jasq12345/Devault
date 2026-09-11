package dev.devault.ingestion.client.github.dto

import java.time.Instant

data class RateLimitInfo(
    val remaining: Int,
    val resetAt: Instant,
    val cost: Int
)