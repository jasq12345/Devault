package dev.devault.auth.service

import dev.devault.auth.dto.request.LoginDto
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

    fun register(user: User): User{
        user.password = passwordEncoder.encode(user.password)
        user.enabled = true
        return userRepository.save(user)
    }

    fun login(dto: LoginDto): String{

        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                dto.username,
                dto.password
            )
        )

        val principal = authentication.principal as? UserPrincipal
            ?: throw IllegalStateException("Invalid principal type")

        return jwtService.generateToken(principal)
    }
}