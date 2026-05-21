package dev.devault.auth.model

import jakarta.persistence.*
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
    var authorities: List<String> = listOf(),

    var banned: Boolean = false,
    var enabled: Boolean = false,
) {}