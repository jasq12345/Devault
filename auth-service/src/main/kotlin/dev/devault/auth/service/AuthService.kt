package dev.devault.auth.service

import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
) {

    fun register(user: User): User{
        user.password = passwordEncoder.encode(user.password)
        return userRepository.save(user)
    }

    fun login(user: User){

    }
}