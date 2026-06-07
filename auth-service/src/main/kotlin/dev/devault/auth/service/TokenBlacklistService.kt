package dev.devault.auth.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class TokenBlacklistService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    fun blacklist(jti: UUID, ttl: Long) {
        redisTemplate.opsForValue().set("blacklist:$jti", "1", ttl, TimeUnit.MILLISECONDS)
    }

    fun isBlacklisted(jti: UUID): Boolean {
        return redisTemplate.hasKey("blacklist:$jti") == true
    }
}