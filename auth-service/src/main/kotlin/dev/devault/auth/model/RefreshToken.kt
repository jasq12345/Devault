package dev.devault.auth.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @jakarta.persistence.Column(nullable = false, unique = true)
    var token: String,
    @jakarta.persistence.Column(nullable = false)
    var userId: UUID,
    var createdAt: Instant = Instant.now(),
)
