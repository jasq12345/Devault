package dev.devault.auth.service

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.dto.request.RefreshTokenDto
import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.dto.response.RegisterResponseDto
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.dto.response.toResponse
import dev.devault.auth.exception.InvalidTokenException
import dev.devault.auth.exception.UserAlreadyExistsException
import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principal.UserPrincipal
import dev.devault.authlib.service.JwtClaimsService
import dev.devault.authlib.type.TokenType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtGenerationService: JwtGenerationService,
    private val jwtClaimsService: JwtClaimsService,
    private val blacklistService: TokenBlacklistService
) {

    fun register(dto: RegisterDto) : RegisterResponseDto {
        validateRegisterDto(dto)

        val user = User(
            email = dto.email,
            username = dto.username,
            password = passwordEncoder.encode(dto.password)
        )

        user.enabled = true
        val savedUser = userRepository.save(user)

        return savedUser.toResponse()
    }

    private fun validateRegisterDto(dto: RegisterDto) {
        if (userRepository.existsByUsernameOrEmail(dto.username, dto.email)) {
            throw UserAlreadyExistsException("Username or email already exists")
        }
    }
    fun login(dto: LoginDto): TokenPair {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                dto.identifier,
                dto.password
            )
        )

        val principal = authentication.principal as? UserPrincipal
            ?: throw IllegalStateException("Invalid principal type")

        return jwtGenerationService.generateTokenPair(principal)
    }

    fun refresh(dto: RefreshTokenDto): TokenPair {
        val (jti, ttl) = validateAndExtractRefreshToken(dto.refreshToken)
        blacklistOrThrow(jti, ttl)

        val userId = jwtClaimsService.extractId(dto.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { UsernameNotFoundException("User not found") }

        val principal = UserPrincipal.build(user)

        return jwtGenerationService.generateTokenPair(principal)
    }

    fun logout(dto: RefreshTokenDto) {
        val (jti, ttl) = validateAndExtractRefreshToken(dto.refreshToken)
        blacklistOrThrow(jti, ttl)
    }

    private fun validateAndExtractRefreshToken(token: String): RefreshTokenClaims {
        if (!isRefreshToken(token))
            throw InvalidTokenException("Invalid token type")

        val jti = jwtClaimsService.extractJti(token)

        if (blacklistService.isBlacklisted(jti))
            throw InvalidTokenException("Refresh token is blacklisted")

        val ttl = jwtClaimsService.extractExpiration(token).time - System.currentTimeMillis()
        if (ttl <= 0) throw InvalidTokenException("Refresh token expired")

        return RefreshTokenClaims(jti, ttl)
    }

    private fun blacklistOrThrow(jti: UUID, ttl: Long) {
        if (!blacklistService.blacklist(jti, ttl))
            throw InvalidTokenException("Refresh token is blacklisted")
    }

    private fun isRefreshToken(token: String): Boolean =
        jwtClaimsService.extractClaim(token) { it["type"] } == TokenType.REFRESH.name.lowercase()

    private data class RefreshTokenClaims(val jti: UUID, val ttl: Long)
}
