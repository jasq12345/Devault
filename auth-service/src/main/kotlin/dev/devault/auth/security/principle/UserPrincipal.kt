package dev.devault.auth.security.principle

import dev.devault.auth.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import dev.devault.auth.type.RoleType
import java.util.UUID

class UserPrincipal(
    private val id: UUID,
    private val username: String,
    private val password: String,
    private val authorities: MutableSet<RoleType>,
    private val enabled: Boolean,
    private val banned: Boolean,
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return authorities.map { GrantedAuthority { it.name } }
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

    fun getId(): UUID {
        return id
    }

    companion object{
        fun build(user: User): UserPrincipal{
            return UserPrincipal(
                id = user.id!!,
                password = user.password ?: "",
                username = user.username,
                authorities = user.authorities,
                enabled = user.enabled,
                banned = user.banned
            )
        }
    }
}
