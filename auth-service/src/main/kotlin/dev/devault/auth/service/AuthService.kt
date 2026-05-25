package dev.devault.auth.service

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val authenticationManager: AuthenticationManager,
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

        return "Login successful for user: ${authentication.name}"
    }
}