package dev.devault.auth.service

import dev.devault.auth.model.User
import dev.devault.auth.repository.UserRepository
import dev.devault.auth.security.principle.UserPrinciple
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    val repository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val isUsername = !username.contains('@')

        val user: User = (if (isUsername) repository.findByUsername(username)
            ?: { UsernameNotFoundException("Username or password is incorrect") }
        else repository.findByEmail(username)
            ?: { UsernameNotFoundException("Username or password is incorrect") }) as User

        return UserPrinciple.build(user)
    }

}