package dev.devault.auth.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBlacklistServiceTest {
    private val redisTemplate = mockk<RedisTemplate<String, String>>()
    private val service = TokenBlacklistService(redisTemplate)

    @Nested
    inner class Blacklist {
        @Test
        fun `returns true when key was not already present`() {
            val jti = UUID.randomUUID()
            val valueOperations = mockk<ValueOperations<String, String>>()

            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.setIfAbsent("blacklist:$jti", "1", 60000L, TimeUnit.MILLISECONDS) } returns true

            assertTrue {
                service.blacklist(jti, 60000L)
            }
        }

        @Test
        fun `returns false when key was already present`() {
            val jti = UUID.randomUUID()
            val valueOperations = mockk<ValueOperations<String, String>>()

            every { redisTemplate.opsForValue() } returns valueOperations
            every { valueOperations.setIfAbsent("blacklist:$jti", "1", 60000L, TimeUnit.MILLISECONDS) } returns false

            assertFalse {
                service.blacklist(jti, 60000L)
            }
        }
    }

    @Nested
    inner class IsBlacklisted {
        @Test
        fun `returns true when key exists`() {
            val jti = UUID.randomUUID()

            every { redisTemplate.hasKey("blacklist:$jti") } returns true


            assertTrue {
                service.isBlacklisted(jti)
            }
        }

        @Test
        fun `returns false when key does not exist`() {
            val jti = UUID.randomUUID()

            every { redisTemplate.hasKey("blacklist:$jti") } returns false


            assertFalse {
                service.isBlacklisted(jti)
            }
        }
    }
}