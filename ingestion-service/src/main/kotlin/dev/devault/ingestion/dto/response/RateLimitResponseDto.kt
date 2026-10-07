package dev.devault.ingestion.dto.response

import dev.devault.ingestion.client.github.dto.RateLimitInfo
import java.time.Instant

data class RateLimitResponseDto(
    val remaining: Int,
    val resetAt: Instant
)

fun RateLimitInfo.toResponse() = RateLimitResponseDto(
    remaining = remaining,
    resetAt = resetAt
)
