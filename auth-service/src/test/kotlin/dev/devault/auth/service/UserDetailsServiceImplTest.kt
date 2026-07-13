package dev.devault.auth.service

import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.type.RoleType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.UUID
import kotlin.test.assertEquals

class UserDetailsServiceImplTest {
    private val userRepository = mockk<UserRepository>()
    private val service = UserDetailsServiceImpl(userRepository)

    @Nested
    inner class LoadUserByUsername {
        @Test
        fun `returns user principal when identifier is a username`() {
            val identifier = "username"
            val user = User(
                UUID.randomUUID(),
                identifier,
                "email@email.com",
                "password",
                mutableSetOf(RoleType.STANDARD),
                banned = false,
                enabled = true
            )

            every { userRepository.findByUsername(identifier) } returns user

            val result = service.loadUserByUsername(identifier)

            assertEquals(user.id, result.getId())
        }

        @Test
        fun `returns user principal when identifier is an email`() {
            val identifier = "email@email.com"
            val user = User(
                UUID.randomUUID(),
                "username",
                identifier,
                "password",
                mutableSetOf(RoleType.STANDARD),
                banned = false,
                enabled = true
            )

            every { userRepository.findByEmail(identifier) } returns user

            val result = service.loadUserByUsername(identifier)

            assertEquals(user.id, result.getId())

        }

        @Test
        fun `throws when username does not exist`() {
            val identifier = "username"

            every { userRepository.findByUsername(identifier) } returns null

            assertThrows<UsernameNotFoundException> {
                service.loadUserByUsername(identifier)
            }
        }

        @Test
        fun `throws when email does not exist`() {
            val identifier = "email@email.com"

            every { userRepository.findByEmail(identifier) } returns null

            assertThrows<UsernameNotFoundException> {
                service.loadUserByUsername(identifier)
            }
        }
    }
}