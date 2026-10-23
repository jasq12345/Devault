package dev.devault.commonlib.type

// Shared by workspace-service, which owns the roles, and the services that ask it for a member's role.
enum class WorkspaceRole {
    MEMBER,
    ADMIN,
    OWNER
}