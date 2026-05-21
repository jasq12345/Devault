package dev.devault.auth.security.principle

import dev.devault.auth.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class UserPrinciple(
    private val id: UUID,
    private val username: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val enabled: Boolean,
    private val banned: Boolean,
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return authorities
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonLocked(): Boolean
    {
        return !banned
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    companion object{
        fun build(user: User): UserPrinciple{
            val authorities = mutableListOf<GrantedAuthority>()
            return UserPrinciple(
                id = user.id!!,
                password = user.password ?: "",
                username = user.username,
                authorities = authorities,
                enabled = user.enabled,
                banned = user.banned
            )
        }
    }
}
