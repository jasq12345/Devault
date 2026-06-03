package dev.devault.auth.model

import jakarta.persistence.*
import dev.devault.auth.type.RoleType
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(unique = true, nullable = false)
    var username: String,

    @Column(unique = true, nullable = false)
    var email: String,

    var password: String? = null,


    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    var authorities: MutableSet<RoleType> = mutableSetOf(RoleType.STANDARD),

    var banned: Boolean = false,
    var enabled: Boolean = false,
)