package dev.devault.auth.service

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.dto.request.RefreshTokenDto
import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.dto.response.RegisterResponseDto
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.model.RefreshToken
import dev.devault.auth.model.User
import dev.devault.auth.repository.RefreshTokenRepository
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principal.UserPrincipal
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
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

        return RegisterResponseDto(
            id = savedUser.id,
            email = savedUser.email,
            username = savedUser.username,
            authorities = savedUser.authorities
        )
    }

    private fun validateRegisterDto(dto: RegisterDto) {
        if (userRepository.existsByUsername(dto.username)) {
            throw IllegalStateException("Username already exists")
        }
        if (userRepository.existsByEmail(dto.email)) {
            throw IllegalStateException("Email already exists")
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

        val tokenPair = jwtService.generateTokenPair(principal)
        saveRefreshToken(tokenPair.refreshToken, principal.getId())

        return tokenPair
    }

    fun refresh(dto: RefreshTokenDto): TokenPair {
        if(!jwtService.isRefreshToken(dto.refreshToken))
            throw IllegalStateException("Invalid token type")

        refreshTokenRepository.findByToken(dto.refreshToken)
            ?: throw IllegalStateException("Refresh token not found")

        refreshTokenRepository.deleteByToken(dto.refreshToken)

        val userId: UUID = jwtService.extractId(dto.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("User not found") }

        val principal = UserPrincipal.build(user)
        val tokenPair = jwtService.generateTokenPair(principal)

        saveRefreshToken(tokenPair.refreshToken, userId)

        return tokenPair
    }

    fun logout(dto: RefreshTokenDto) {
        if(!jwtService.isRefreshToken(dto.refreshToken))
            throw IllegalStateException("Invalid token type")

        refreshTokenRepository.deleteByToken(dto.refreshToken)
    }


    private fun saveRefreshToken(token: String, userId: UUID) {
        refreshTokenRepository.save(RefreshToken(token = token, userId = userId))
    }
}