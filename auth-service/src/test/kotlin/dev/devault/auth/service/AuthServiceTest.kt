package dev.devault.auth.service

import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.exception.UserAlreadyExistsException
import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.type.RoleType
import dev.devault.authlib.service.JwtClaimsService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID
import kotlin.test.assertEquals

class AuthServiceTest {
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val userRepository = mockk<UserRepository>()
    private val authenticationManager = mockk<AuthenticationManager>()
    private val jwtGenerationService = mockk<JwtGenerationService>()
    private val jwtClaimsService = mockk<JwtClaimsService>()
    private val blacklistService = mockk<TokenBlacklistService>()

    private val service = AuthService(
        passwordEncoder,
        userRepository,
        authenticationManager,
        jwtGenerationService,
        jwtClaimsService,
        blacklistService
    )

    @Nested
    inner class Register {
        private val dto = RegisterDto("email@email.com", "username", "password")

        @Test
        fun `creates user when username and email are unique`() {
            val user = User(
                UUID.randomUUID(),
                "username",
                "email@email.com",
                "hashedPassword",
                mutableSetOf(RoleType.STANDARD),
                banned = false,
                enabled = true
            )

            every { userRepository.existsByUsernameOrEmail(dto.username, dto.email) } returns false
            every { passwordEncoder.encode(dto.password) } returns "hashedPassword"
            every { userRepository.save(any()) } returns user

            val result = service.register(dto)

            assertEquals(user.email, result.email)
            assertEquals(user.username, result.username)
            assertEquals(user.id, result.id)
        }

        @Test
        fun `throws when username or email already exists`() {
            every { userRepository.existsByUsernameOrEmail(dto.username, dto.email) } returns true

            assertThrows<UserAlreadyExistsException> {
                service.register(dto)
            }
        }
    }

    @Nested
    inner class Login {
        @Test
        fun `returns token pair when credentials are valid`() {
        }

        @Test
        fun `throws when principal is not a UserPrincipal`() {
        }
    }

    @Nested
    inner class Refresh {
        @Test
        fun `returns new token pair when refresh token is valid`() {
        }

        @Test
        fun `throws when token is not a refresh token`() {
        }

        @Test
        fun `throws when refresh token is already blacklisted`() {
        }

        @Test
        fun `throws when refresh token is expired`() {
        }

        @Test
        fun `throws when blacklisting fails`() {
        }

        @Test
        fun `throws when user does not exist`() {
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `blacklists token when refresh token is valid`() {
        }

        @Test
        fun `throws when token is not a refresh token`() {
        }

        @Test
        fun `throws when refresh token is already blacklisted`() {
        }

        @Test
        fun `throws when refresh token is expired`() {
        }
    }
}