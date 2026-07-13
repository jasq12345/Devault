package dev.devault.auth.service

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.dto.request.RefreshTokenDto
import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.exception.InvalidTokenException
import dev.devault.auth.exception.UserAlreadyExistsException
import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principal.UserPrincipal
import dev.devault.auth.type.RoleType
import dev.devault.authlib.service.JwtClaimsService
import dev.devault.authlib.type.TokenType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Date
import java.util.Optional
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
        private val dto = LoginDto("identifier", "password")

        @Test
        fun `returns token pair when credentials are valid`() {
            val authentication = mockk<Authentication>()
            val userPrincipal = mockk<UserPrincipal>()

            every { authenticationManager.authenticate(any()) } returns authentication
            every { authentication.principal } returns userPrincipal
            every { jwtGenerationService.generateTokenPair(userPrincipal) } returns TokenPair("accessToken", "refreshToken")

            val result = service.login(dto)

            assertEquals("accessToken", result.accessToken)
            assertEquals("refreshToken", result.refreshToken)
        }

        @Test
        fun `throws when principal is not a UserPrincipal`() {
            val authentication = mockk<Authentication>()

            every { authenticationManager.authenticate(any()) } returns authentication
            every { authentication.principal } returns "not UserPrinciple"

            assertThrows<IllegalStateException> {
                service.login(dto)
            }
        }
    }

    @Nested
    inner class Refresh {
        private val dto = RefreshTokenDto("refreshToken")

        @Test
        fun `returns new token pair when refresh token is valid`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()
            val futureDate = Date(System.currentTimeMillis() + 60_000)
            val userId = UUID.randomUUID()
            val user = User(
                userId,
                "username",
                "email@email.com",
                "hashedPassword",
                mutableSetOf(RoleType.STANDARD),
                banned = false,
                enabled = true
            )

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns false
            every { jwtClaimsService.extractExpiration(token) } returns futureDate
            every { blacklistService.blacklist(jti, any()) } returns true
            every { jwtClaimsService.extractId(dto.refreshToken) } returns userId
            every { userRepository.findById(userId) } returns Optional.of(user)
            every { jwtGenerationService.generateTokenPair(any()) } returns TokenPair("accessToken", "refreshToken")

            val result = service.refresh(dto)

            assertEquals("accessToken", result.accessToken)
            assertEquals("refreshToken", result.refreshToken)
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
                jwtClaimsService.extractExpiration(token)
                blacklistService.blacklist(jti, any())
                jwtClaimsService.extractId(dto.refreshToken)
                userRepository.findById(userId)
                jwtGenerationService.generateTokenPair(any())
            }
        }

        @Test
        fun `throws when token is not a refresh token`() {
            val token = dto.refreshToken

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.ACCESS.name.lowercase()

            assertThrows<InvalidTokenException> {
                service.refresh(dto)
            }
        }

        @Test
        fun `throws when refresh token is already blacklisted`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns true

            assertThrows<InvalidTokenException> {
                service.refresh(dto)
            }
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
            }
        }

        @Test
        fun `throws when refresh token is expired`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()
            val pastDate = Date(System.currentTimeMillis() - 60_000)

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns false
            every { jwtClaimsService.extractExpiration(token) } returns pastDate

            assertThrows<InvalidTokenException> {
                service.refresh(dto)
            }
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
                jwtClaimsService.extractExpiration(token)
            }
        }

        @Test
        fun `throws when blacklisting fails`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()
            val futureDate = Date(System.currentTimeMillis() + 60_000)

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns false
            every { jwtClaimsService.extractExpiration(token) } returns futureDate
            every { blacklistService.blacklist(jti, any()) } returns false

            assertThrows<InvalidTokenException> {
                service.refresh(dto)
            }
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
                jwtClaimsService.extractExpiration(token)
                blacklistService.blacklist(jti, any())
            }
        }

        @Test
        fun `throws when user does not exist`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()
            val futureDate = Date(System.currentTimeMillis() + 60_000)
            val userId = UUID.randomUUID()

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns false
            every { jwtClaimsService.extractExpiration(token) } returns futureDate
            every { blacklistService.blacklist(jti, any()) } returns true
            every { jwtClaimsService.extractId(dto.refreshToken) } returns userId
            every { userRepository.findById(userId) } returns Optional.empty()

            assertThrows<UsernameNotFoundException> {
                service.refresh(dto)
            }
        }
    }

    @Nested
    inner class Logout {
        private val dto = RefreshTokenDto("refreshToken")

        @Test
        fun `blacklists token when refresh token is valid`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()
            val futureDate = Date(System.currentTimeMillis() + 60_000)

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns false
            every { jwtClaimsService.extractExpiration(token) } returns futureDate
            every { blacklistService.blacklist(jti, any()) } returns true

            service.logout(dto)

            verify { blacklistService.blacklist(jti, any()) }
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
                jwtClaimsService.extractExpiration(token)
                blacklistService.blacklist(jti, any())
            }
        }

        @Test
        fun `throws when token is not a refresh token`() {
            val token = dto.refreshToken

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.ACCESS.name.lowercase()

            assertThrows<InvalidTokenException> {
                service.logout(dto)
            }
        }

        @Test
        fun `throws when refresh token is already blacklisted`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns true

            assertThrows<InvalidTokenException> {
                service.logout(dto)
            }
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
            }
        }

        @Test
        fun `throws when refresh token is expired`() {
            val token = dto.refreshToken
            val jti = UUID.randomUUID()
            val pastDate = Date(System.currentTimeMillis() - 60_000)

            every { jwtClaimsService.extractClaim<String>(token, any()) } returns TokenType.REFRESH.name.lowercase()
            every { jwtClaimsService.extractJti(token) } returns jti
            every { blacklistService.isBlacklisted(jti) } returns false
            every { jwtClaimsService.extractExpiration(token) } returns pastDate

            assertThrows<InvalidTokenException> {
                service.logout(dto)
            }
            verifyOrder {
                jwtClaimsService.extractClaim<String>(token, any())
                jwtClaimsService.extractJti(token)
                blacklistService.isBlacklisted(jti)
                jwtClaimsService.extractExpiration(token)
            }
        }
    }
}