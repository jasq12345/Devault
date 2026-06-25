package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.exception.InvalidOperationException
import dev.devault.workspace.dto.request.SaveWorkspaceDto
import dev.devault.workspace.dto.request.TransferOwnershipDto
import dev.devault.workspace.dto.request.UpdateWorkspaceDto
import dev.devault.workspace.exception.SlugAlreadyExistsException
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.model.WorkspaceMember
import dev.devault.workspace.repository.WorkspaceRepository
import dev.devault.workspace.type.WorkspaceRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import java.util.Optional
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
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val id = UUID.randomUUID()
        @Test
        fun `returns workspace when caller is a member`() {
            val workspace = Workspace(id, "workspace", "workspace", authenticatedUser.id)

            every { repository.findById(id) } returns Optional.of(workspace)
            every { workspaceMemberService.isMember(id, authenticatedUser.id) } returns true

            val result = service.findWorkspaceById(authenticatedUser, id)

            assertEquals(id, result.id)
        }

        @Test
        fun `throws when caller is not a member`() {
            val workspace = Workspace(id, "workspace", "workspace", authenticatedUser.id)

            every { repository.findById(id) } returns Optional.of(workspace)
            every { workspaceMemberService.isMember(id, authenticatedUser.id) } returns false

            assertThrows<AccessDeniedException> {
                service.findWorkspaceById(authenticatedUser, id)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            every { repository.findById(id) } returns Optional.empty()

            assertThrows<NoSuchElementException> {
                service.findWorkspaceById(authenticatedUser, id)
            }
        }
    }

    @Nested
    inner class SaveWorkspace {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val dto = SaveWorkspaceDto("name", "slug")

        @Test
        fun `creates workspace and adds caller as owner`() {
            val workspace = Workspace(UUID.randomUUID(), dto.name, dto.slug, authenticatedUser.id)

            every { repository.existsBySlug(dto.slug) } returns false
            every { repository.save(any()) } returns workspace
            every{ workspaceMemberService.addOwner(workspace, authenticatedUser.id) } returns mockk()

            val result = service.saveWorkspace(authenticatedUser, dto)

            assertEquals(workspace.id, result.id)
            verify { workspaceMemberService.addOwner(workspace, authenticatedUser.id) }
        }

        @Test
        fun `throws when slug already exists`() {
            every { repository.existsBySlug(dto.slug) } returns true

            assertThrows<SlugAlreadyExistsException> {
                service.saveWorkspace(authenticatedUser, dto)
            }
        }
    }

    @Nested
    inner class UpdateWorkspace {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val id = UUID.randomUUID()
        private val dto = UpdateWorkspaceDto("name")

        @Test
        fun `updates name when caller is owner`() {
            val workspace = Workspace(id, "old name", "slug", authenticatedUser.id)
            every { repository.findById(id)} returns Optional.of(workspace)
            every { repository.save(workspace) } returns workspace

            val result = service.updateWorkspace(authenticatedUser, dto, id)

            assertEquals(dto.name, result.name)
        }

        @Test
        fun `throws when caller is not owner`() {
            val workspace = Workspace(id, "old name", "slug", UUID.randomUUID())

            every { repository.findById(id) } returns Optional.of(workspace)

            assertThrows<AccessDeniedException> {
                service.updateWorkspace(authenticatedUser, dto, id)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            every { repository.findById(id) } returns Optional.empty()

            assertThrows<NoSuchElementException> {
                service.updateWorkspace(authenticatedUser, dto, id)
            }
        }
    }

    @Nested
    inner class DeleteWorkspace {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val id = UUID.randomUUID()

        @Test
        fun `removes members before deleting workspace when caller is owner`() {
            val workspace = Workspace(id, "old name", "slug", authenticatedUser.id)

            every { repository.findById(id) } returns Optional.of(workspace)
            every { workspaceMemberService.deleteAllByWorkspaceId(id) } returns Unit
            every { repository.delete(workspace) } returns Unit

            service.deleteWorkspace(authenticatedUser, id)

            verifyOrder {
                workspaceMemberService.deleteAllByWorkspaceId(id)
                repository.delete(workspace)
            }
        }

        @Test
        fun `throws when caller is not owner`() {
            val workspace = Workspace(id, "old name", "slug", UUID.randomUUID())

            every { repository.findById(id) } returns Optional.of(workspace)

            assertThrows<AccessDeniedException> {
                service.deleteWorkspace(authenticatedUser, id)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            every { repository.findById(id) } returns Optional.empty()

            assertThrows<NoSuchElementException> {
                service.deleteWorkspace(authenticatedUser, id)
            }
        }
    }

    @Nested
    inner class ChangeOwner {
        @Test
        fun `updates ownerId and saves workspace`() {
            val workspace = Workspace(UUID.randomUUID(), "old name", "slug", UUID.randomUUID())
            val ownerId = UUID.randomUUID()

            every { repository.save(workspace) } returns workspace

            service.changeOwner(workspace, ownerId)

            assertEquals(ownerId, workspace.ownerId)
            verify { repository.save(workspace) }
        }
    }

    @Nested
    inner class TransferOwnership {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()

        @Test
        fun `transfers ownership when caller is owner and target is a member`() {
            val dto = TransferOwnershipDto(UUID.randomUUID())
            val workspace = Workspace(workspaceId, "name", "slug", authenticatedUser.id)
            val currentOwner = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val newOwner = WorkspaceMember(UUID.randomUUID(), dto.newOwnerId, WorkspaceRole.MEMBER, workspace)
            val members = mutableListOf(currentOwner, newOwner)

            every { workspaceMemberService.findAllByWorkspaceId(workspaceId) } returns members
            every { workspaceMemberService.saveMember(newOwner) } returns newOwner
            every { workspaceMemberService.saveMember(currentOwner) } returns currentOwner
            every { repository.save(any()) } returns workspace

            val result = service.transferOwnership(authenticatedUser, workspaceId, dto)

            assertEquals( WorkspaceRole.ADMIN, result.first().role)
            assertEquals(WorkspaceRole.OWNER, result.last().role)
            assertEquals(2, result.size)
            verifyOrder {
                workspaceMemberService.findAllByWorkspaceId(workspaceId)
                workspaceMemberService.saveMember(newOwner)
                workspaceMemberService.saveMember(currentOwner)
                repository.save(any())
            }
        }

        @Test
        fun `throws when transferring to yourself`() {
            val dto = TransferOwnershipDto(authenticatedUser.id)

            assertThrows<IllegalArgumentException> {
                service.transferOwnership(authenticatedUser, workspaceId, dto)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            val dto = TransferOwnershipDto(UUID.randomUUID())
            val members = mutableListOf<WorkspaceMember>()

            every { workspaceMemberService.findAllByWorkspaceId(workspaceId) } returns members

            assertThrows<NoSuchElementException> {
                service.transferOwnership(authenticatedUser, workspaceId, dto)
            }
        }

        @Test
        fun `throws when caller is not a member`() {
            val dto = TransferOwnershipDto(UUID.randomUUID())
            val workspace = Workspace(workspaceId, "name", "slug", authenticatedUser.id)
            val someoneElse = WorkspaceMember(UUID.randomUUID(), UUID.randomUUID(), WorkspaceRole.OWNER, workspace)
            val members = mutableListOf(someoneElse)

            every { workspaceMemberService.findAllByWorkspaceId(workspaceId) } returns members

            assertThrows<AccessDeniedException> {
                service.transferOwnership(authenticatedUser, workspaceId, dto)
            }
        }

        @Test
        fun `throws when caller is not owner`() {
            val dto = TransferOwnershipDto(UUID.randomUUID())
            val workspace = Workspace(workspaceId, "name", "slug", authenticatedUser.id)
            val currentOwner = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.MEMBER, workspace)
            val members = mutableListOf(currentOwner)

            every { workspaceMemberService.findAllByWorkspaceId(workspaceId) } returns members

            assertThrows<AccessDeniedException> {
                service.transferOwnership(authenticatedUser, workspaceId, dto)
            }
        }

        @Test
        fun `throws when new owner is not a member`() {
            val dto = TransferOwnershipDto(UUID.randomUUID())
            val workspace = Workspace(workspaceId, "name", "slug", authenticatedUser.id)
            val currentOwner = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val newOwner = WorkspaceMember(UUID.randomUUID(), UUID.randomUUID(), WorkspaceRole.MEMBER, workspace)
            val members = mutableListOf(currentOwner, newOwner)

            every { workspaceMemberService.findAllByWorkspaceId(workspaceId) } returns members

            assertThrows<InvalidOperationException> {
                service.transferOwnership(authenticatedUser, workspaceId, dto)
            }
        }
    }
}