package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.model.WorkspaceMember
import dev.devault.workspace.repository.WorkspaceRepository
import dev.devault.workspace.type.WorkspaceRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals


class WorkspaceServiceTest {
    private val repository = mockk<WorkspaceRepository>()
    private val workspaceMemberService = mockk<WorkspaceMemberService>()
    private val service = WorkspaceService(repository, workspaceMemberService)

    @Nested
    inner class FindAllWorkspaces {
        @Test
        fun `returns workspaces for memberships of the user`() {
            val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
            val workspace = Workspace(UUID.randomUUID(), "workspace", "workspace", authenticatedUser.id)
            val membership = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val memberships = mutableListOf(membership)

            every { workspaceMemberService.findWorkspacesByUserId(authenticatedUser.id) } returns memberships

            val result = service.findAllWorkspaces(authenticatedUser)

            assertEquals(1, result.size)
            assertEquals(workspace.id, result.first().id)
        }
    }

    @Nested
    inner class FindWorkspaceById {
        @Test
        fun `returns workspace when caller is a member`() {
            val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())

        }

        @Test
        fun `throws when caller is not a member`() {
        }

        @Test
        fun `throws when workspace does not exist`() {
        }
    }

    @Nested
    inner class SaveWorkspace {
        @Test
        fun `creates workspace and adds caller as owner`() {
        }

        @Test
        fun `throws when slug already exists`() {
        }
    }

    @Nested
    inner class UpdateWorkspace {
        @Test
        fun `updates name when caller is owner`() {
        }

        @Test
        fun `throws when caller is not owner`() {
        }

        @Test
        fun `throws when workspace does not exist`() {
        }
    }

    @Nested
    inner class DeleteWorkspace {
        @Test
        fun `removes members before deleting workspace when caller is owner`() {
        }

        @Test
        fun `throws when caller is not owner`() {
        }

        @Test
        fun `throws when workspace does not exist`() {
        }
    }

    @Nested
    inner class ChangeOwner {
        @Test
        fun `updates ownerId and saves workspace`() {
        }
    }

    @Nested
    inner class TransferOwnership {
        @Test
        fun `transfers ownership when caller is owner and target is a member`() {
        }

        @Test
        fun `throws when transferring to yourself`() {
        }

        @Test
        fun `throws when workspace does not exist`() {
        }

        @Test
        fun `throws when caller is not a member`() {
        }

        @Test
        fun `throws when caller is not owner`() {
        }

        @Test
        fun `throws when new owner is not a member`() {
        }
    }
}