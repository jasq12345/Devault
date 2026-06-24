package dev.devault.workspace.service

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.dto.request.SaveWorkspaceMemberDto
import dev.devault.workspace.dto.request.UpdateWorkspaceMemberRoleDto
import dev.devault.workspace.exception.CannotModifyOwnerException
import dev.devault.workspace.exception.CannotRemoveProtectedRoleException
import dev.devault.workspace.exception.UserAlreadyMemberException
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.model.WorkspaceMember
import dev.devault.workspace.repository.WorkspaceMemberRepository
import dev.devault.workspace.type.WorkspaceRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WorkspaceMemberServiceTest {
    private val repository = mockk<WorkspaceMemberRepository>()
    private val service = WorkspaceMemberService(repository)

    @Nested
    inner class IsMember {

        @Test
        fun `returns true when user is member of the workspace`() {
            val workspaceId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            every { repository.existsByWorkspaceIdAndUserId(workspaceId, userId) } returns true

            val result = service.isMember(workspaceId, userId)

            assertTrue(result)
        }

        @Test
        fun `returns false when user is not a member of the workspace`() {
            val workspaceId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            every { repository.existsByWorkspaceIdAndUserId(workspaceId, userId) } returns false

            val result = service.isMember(workspaceId, userId)

            assertFalse(result)
        }
    }

    @Nested
    inner class SaveMember {
        @Test
        fun `returns workspaceMember when member is saved`() {
            val member = mockk<WorkspaceMember>()
            every { repository.save(member) } returns member

            val result = service.saveMember(member)

            assertEquals(member, result)
        }
    }

    @Nested
    inner class FindAllByWorkspaceId {
        @Test
        fun `returns list of members from repository`() {
            val workspaceId = UUID.randomUUID()
            val members = mutableListOf(mockk<WorkspaceMember>(), mockk<WorkspaceMember>())

            every { repository.findAllByWorkspaceId(workspaceId) } returns members

            val result = service.findAllByWorkspaceId(workspaceId)

            assertEquals(members, result)
        }
    }

    @Nested
    inner class DeleteAllByWorkspaceId {
        @Test
        fun `delegates to repository`() {
            val workspaceId = UUID.randomUUID()

            every { repository.deleteByWorkspaceId(workspaceId) } returns Unit

            service.deleteAllByWorkspaceId(workspaceId)

            verify { repository.deleteByWorkspaceId(workspaceId) }
        }
    }

    @Nested
    inner class FindWorkspacesByUserId {
        @Test
        fun `returns list of memberships from repository`() {
            val userId = UUID.randomUUID()
            val memberships = mutableListOf(mockk<WorkspaceMember>(), mockk<WorkspaceMember>())

            every { repository.findByUserId(userId) } returns memberships

            val result = service.findWorkspacesByUserId(userId)

            assertEquals(memberships, result)
        }
    }

    @Nested
    inner class AddOwner {
        @Test
        fun `return workspace member from repository`() {
            val workspace = mockk<Workspace>()
            val userId = UUID.randomUUID()

            val workspaceMember = WorkspaceMember(UUID.randomUUID(), userId, WorkspaceRole.OWNER, workspace)

            every { repository.save(any()) } returns workspaceMember

            val result = service.addOwner(workspace, userId)

            assertEquals(workspaceMember, result)
        }
    }

    @Nested
    inner class FindAllMembers {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val workspace = mockk<Workspace>()
        init{
            every { workspace.id } returns workspaceId
        }

        @Test
        fun `returns list of members when caller is a member`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val members = mutableListOf(caller)

            every { repository.findAllByWorkspaceId(workspaceId) } returns members

            val result = service.findAllMembers(authenticatedUser, workspaceId)

            assertEquals(1, result.size)
            assertEquals(caller.userId, result.first().userId)
        }

        @Test
        fun `throws when caller is not a member`() {
            val someoneElse = WorkspaceMember(UUID.randomUUID(), UUID.randomUUID(), WorkspaceRole.OWNER, workspace)
            val members = mutableListOf(someoneElse)

            every { repository.findAllByWorkspaceId(workspaceId) } returns members

            assertThrows<AccessDeniedException> {
                service.findAllMembers(authenticatedUser, workspaceId)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            every { repository.findAllByWorkspaceId(workspaceId) } returns mutableListOf()

            assertThrows<NoSuchElementException> {
                service.findAllMembers(authenticatedUser, workspaceId)
            }
        }
    }

    @Nested
    inner class FindWorkspaceMemberById {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val id = UUID.randomUUID()
        private val workspace = mockk<Workspace>()
        init {
            every { workspace.id } returns workspaceId
        }

        @Test
        fun `returns member when caller is a member of the workspace`() {
            val member = WorkspaceMember(id, id, WorkspaceRole.MEMBER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.existsByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns true
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns member

            val result = service.findWorkspaceMemberById(authenticatedUser, workspaceId, id)

            assertEquals(member.userId, result.userId)
        }

        @Test
        fun `throws when caller is not a member`() {
            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.existsByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns false

            assertThrows<AccessDeniedException> {
                service.findWorkspaceMemberById(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            every { repository.existsByWorkspaceId(workspaceId) } returns false

            assertThrows<NoSuchElementException> {
                service.findWorkspaceMemberById(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when target member does not exist`() {
            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.existsByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns true
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns null

            assertThrows<NoSuchElementException> {
                service.findWorkspaceMemberById(authenticatedUser, workspaceId, id)
            }
        }
    }

    @Nested
    inner class SaveWorkspaceMember {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val workspace = mockk<Workspace>()
        init {
            every { workspace.id } returns workspaceId
        }

        @Test
        fun `adds member when caller is owner`() {

            val dto = SaveWorkspaceMemberDto(UUID.randomUUID())
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val members = mutableListOf(caller)

            every { repository.findAllByWorkspaceId(workspaceId) } returns members
            every { repository.save(any()) } returns WorkspaceMember(UUID.randomUUID(), dto.userId, WorkspaceRole.MEMBER, workspace)

            val result = service.saveWorkspaceMember(authenticatedUser, workspaceId, dto)

            assertEquals(dto.userId, result.userId)
            assertEquals(WorkspaceRole.MEMBER, result.role)
        }

        @Test
        fun `throws when workspace does not exist`() {
            val dto = SaveWorkspaceMemberDto(UUID.randomUUID())
            every { repository.findAllByWorkspaceId(workspaceId) } returns mutableListOf()

            assertThrows<NoSuchElementException> {
                service.saveWorkspaceMember(authenticatedUser, workspaceId, dto)
            }
        }

        @Test
        fun `throws when caller lacks permission`() {
            val dto = SaveWorkspaceMemberDto(UUID.randomUUID())
            val member = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.MEMBER, workspace)
            every { repository.findAllByWorkspaceId(workspaceId) } returns mutableListOf(member)

            assertThrows<AccessDeniedException> {
                service.saveWorkspaceMember(authenticatedUser, workspaceId, dto)
            }
        }

        @Test
        fun `throws when user is already a member`() {
            val dto = SaveWorkspaceMemberDto(authenticatedUser.id)
            val member = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.ADMIN, workspace)
            every { repository.findAllByWorkspaceId(workspaceId) } returns mutableListOf(member)

            assertThrows<UserAlreadyMemberException> {
                service.saveWorkspaceMember(authenticatedUser, workspaceId, dto)
            }
        }
    }

    @Nested
    inner class UpdateMemberRole {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val id = UUID.randomUUID()

        @Test
        fun `promotes member to admin when caller is admin`() {
            val workspace = Workspace(workspaceId, "workspace", "workspace", UUID.randomUUID())
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.ADMIN, workspace)
            val member = WorkspaceMember(UUID.randomUUID(), id, WorkspaceRole.MEMBER, workspace)
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.ADMIN)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns member
            every { repository.save(member) } returns member

            val result = service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)

            assertEquals(WorkspaceRole.ADMIN, result.role)
            assertEquals(WorkspaceRole.ADMIN, member.role)
        }

        @Test
        fun `degrades admin to member when caller is owner`() {
            val workspace = Workspace(workspaceId, "workspace", "workspace", authenticatedUser.id)
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val admin = WorkspaceMember(UUID.randomUUID(), id, WorkspaceRole.ADMIN, workspace)
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.MEMBER)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns admin
            every { repository.save(admin) } returns admin

            val result = service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)

            assertEquals(WorkspaceRole.MEMBER, result.role)
            assertEquals(WorkspaceRole.MEMBER, admin.role)
        }

        @Test
        fun `promotes member to admin when caller is owner`() {
            val workspace = Workspace(workspaceId, "workspace", "workspace", authenticatedUser.id)
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val member = WorkspaceMember(UUID.randomUUID(), id, WorkspaceRole.MEMBER, workspace)
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.ADMIN)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns member
            every { repository.save(member) } returns member

            val result = service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)

            assertEquals(WorkspaceRole.ADMIN, result.role)
            assertEquals(WorkspaceRole.ADMIN, member.role)
        }

        @Test
        fun `throws when admin tries to degrade another admin`() {
            val workspace = Workspace(workspaceId, "workspace", "workspace", UUID.randomUUID())
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.ADMIN, workspace)
            val admin = WorkspaceMember(UUID.randomUUID(), id, WorkspaceRole.ADMIN, workspace)
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.MEMBER)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns admin

            assertThrows<AccessDeniedException> {
                service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)
            }
        }

        @Test
        fun `throws when owner tries to grant owner role`() {
            val workspace = Workspace(workspaceId, "workspace", "workspace", authenticatedUser.id)
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val admin = WorkspaceMember(UUID.randomUUID(), id, WorkspaceRole.ADMIN, workspace)
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.OWNER)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns admin

            assertThrows<AccessDeniedException> {
                service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)
            }
        }

        @Test
        fun `throws when caller tries to change another owner's role`() {
            // Defensive test - can't have two owners :)
            val workspace = Workspace(workspaceId, "workspace", "workspace", authenticatedUser.id)
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val anotherOwner = WorkspaceMember(UUID.randomUUID(), id, WorkspaceRole.OWNER, workspace)
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.ADMIN)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns anotherOwner

            assertThrows<CannotModifyOwnerException> {
                service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.ADMIN)

            every { repository.existsByWorkspaceId(workspaceId) } returns false

            assertThrows<NoSuchElementException> {
                service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)
            }
        }

        @Test
        fun `throws when caller is not a member of the workspace`() {
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.ADMIN)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns null

            assertThrows<AccessDeniedException> {
                service.updateMemberRole(authenticatedUser, workspaceId, id, newRole)
            }
        }

        @Test
        fun `throws when caller is a plain member`() {
            val newRole = UpdateWorkspaceMemberRoleDto(WorkspaceRole.ADMIN)
            val workspace = Workspace(workspaceId, "workspace", "workspace", authenticatedUser.id)
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.MEMBER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller

            assertThrows<AccessDeniedException> {
                service.updateMemberRole(authenticatedUser, workspaceId,  id, newRole)
            }
        }
    }

    @Nested
    inner class DeleteWorkspaceMember {
        private val authenticatedUser = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        private val workspaceId = UUID.randomUUID()
        private val id = UUID.randomUUID()
        private val workspace = mockk<Workspace>()
        init {
            every { workspace.id } returns workspaceId
        }

        @Test
        fun `removes member when caller is owner`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val member = WorkspaceMember(id, UUID.randomUUID(), WorkspaceRole.MEMBER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns member
            every { repository.delete(member) } returns Unit

            service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)

            verify { repository.delete(member) }
        }

        @Test
        fun `removes member when caller is admin`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.ADMIN, workspace)
            val member = WorkspaceMember(id, UUID.randomUUID(), WorkspaceRole.MEMBER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns member
            every { repository.delete(member) } returns Unit

            service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)

            verify { repository.delete(member) }
        }

        @Test
        fun `throws when caller is a plain member`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.MEMBER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller

            assertThrows<AccessDeniedException> {
                service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when caller is not a member`() {
            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns null

            assertThrows<AccessDeniedException> {
                service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when workspace does not exist`() {
            every { repository.existsByWorkspaceId(workspaceId) } returns false

            assertThrows<NoSuchElementException> {
                service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when target member does not exist`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns null

            assertThrows<NoSuchElementException> {
                service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when target is admin`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val targetAdmin = WorkspaceMember(id, UUID.randomUUID(), WorkspaceRole.ADMIN, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns targetAdmin

            assertThrows<CannotRemoveProtectedRoleException> {
                service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
            }
        }

        @Test
        fun `throws when target is owner`() {
            val caller = WorkspaceMember(UUID.randomUUID(), authenticatedUser.id, WorkspaceRole.OWNER, workspace)
            val targetOwner = WorkspaceMember(id, UUID.randomUUID(), WorkspaceRole.OWNER, workspace)

            every { repository.existsByWorkspaceId(workspaceId) } returns true
            every { repository.findWorkspaceMemberByWorkspaceIdAndUserId(workspaceId, authenticatedUser.id) } returns caller
            every { repository.findByIdAndWorkspaceId(id, workspaceId) } returns targetOwner

            assertThrows<CannotRemoveProtectedRoleException> {
                service.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
            }
        }
    }
}