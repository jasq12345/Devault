package dev.devault.auth.service

import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principal.UserPrincipal
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(identifier: String): UserPrincipal {
        val isUsername = !identifier.contains('@')
        val user: User = if (isUsername)
            userRepository.findByUsername(identifier)
                ?: throw UsernameNotFoundException("Username or password is incorrect")
        else
            userRepository.findByEmail(identifier)
                ?: throw UsernameNotFoundException("Username or password is incorrect")

        return UserPrincipal.build(user)
    }
}