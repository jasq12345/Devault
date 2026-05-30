package dev.devault.auth.service

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principle.UserPrincipal
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
) {

    fun register(dto: RegisterDto): User {
        validateRegisterDto(dto)

        val user = User(
            email = dto.email,
            username = dto.username,
            password = passwordEncoder.encode(dto.password)
        )

        user.enabled = true
        return userRepository.save(user)
    }

    private fun validateRegisterDto(dto: RegisterDto) {
        if (userRepository.existsByUsername(dto.username)) {
            throw IllegalStateException("Username already exists")
        }
        if (userRepository.existsByEmail(dto.email)) {
            throw IllegalStateException("Email already exists")
        }
    }
    fun login(dto: LoginDto): String{

        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                dto.identifier,
                dto.password
            )
        )

        val principal = authentication.principal as? UserPrincipal
            ?: throw IllegalStateException("Invalid principal type")

        return jwtService.generateToken(principal)
    }
}