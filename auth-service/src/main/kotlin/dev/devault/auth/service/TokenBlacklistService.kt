package dev.devault.auth.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class TokenBlacklistService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    fun blacklist(jti: UUID, ttl: Long): Boolean {
        return redisTemplate.opsForValue()
            .setIfAbsent("blacklist:$jti", "1", Duration.ofMillis(ttl)) == true
    }

    fun isBlacklisted(jti: UUID): Boolean {
        return redisTemplate.hasKey("blacklist:$jti") == true
    }
}