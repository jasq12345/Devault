package dev.devault.auth.service

import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principle.UserPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val useRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val isUsername = !username.contains('@')
        val user: User = if (isUsername)
            useRepository.findByUsername(username)
                ?: throw UsernameNotFoundException("Username or password is incorrect")
        else
            useRepository.findByEmail(username)
                ?: throw UsernameNotFoundException("Username or password is incorrect")

        return UserPrincipal.build(user)
    }
}