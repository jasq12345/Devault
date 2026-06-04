package dev.devault.auth.repository

import dev.devault.auth.model.RefreshToken
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository  : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?
    @Transactional

    fun deleteAllByUserId(userId: UUID)
    @Transactional

    fun deleteByToken(token: String)
}