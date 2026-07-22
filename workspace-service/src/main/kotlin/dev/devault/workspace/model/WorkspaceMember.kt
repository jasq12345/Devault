package dev.devault.workspace.model

import dev.devault.workspace.type.WorkspaceRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "workspace_members",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["workspace_id", "user_id"])
    ]
)
class WorkspaceMember(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, name = "user_id")
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER,

    @ManyToOne
    @JoinColumn(name = "workspace_id")
    var workspace: Workspace
)