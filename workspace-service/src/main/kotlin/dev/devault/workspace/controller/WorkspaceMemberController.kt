package dev.devault.workspace.controller

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.response.ApiResponse
import dev.devault.workspace.dto.request.SaveWorkspaceMemberDto
import dev.devault.workspace.dto.request.TransferOwnershipDto
import dev.devault.workspace.dto.request.UpdateWorkspaceMemberRoleDto
import dev.devault.workspace.dto.response.WorkspaceMemberResponseDto
import dev.devault.workspace.service.WorkspaceMemberService
import dev.devault.workspace.service.WorkspaceService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
class WorkspaceMemberController(
    private val workspaceMemberService: WorkspaceMemberService,
    private val workspaceService: WorkspaceService
) {
    @GetMapping("")
    fun findAllMembers(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID
    ): ResponseEntity<ApiResponse<List<WorkspaceMemberResponseDto>>> {
        return ResponseEntity.ok(ApiResponse.ok(workspaceMemberService.findAllMembers(authenticatedUser, workspaceId)))
    }

    @GetMapping("/{id}")
    fun findWorkspaceMemberById(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<WorkspaceMemberResponseDto>> {
        return ResponseEntity.ok(ApiResponse.ok(workspaceMemberService.findWorkspaceMemberById(authenticatedUser, workspaceId, id)))
    }

    @PostMapping("")
    fun saveWorkspaceMember(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @RequestBody dto: SaveWorkspaceMemberDto
    ): ResponseEntity<ApiResponse<WorkspaceMemberResponseDto>> {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(workspaceMemberService.saveWorkspaceMember(authenticatedUser, workspaceId, dto)))
    }

    @DeleteMapping("/{id}")
    fun deleteWorkspaceMember(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        workspaceMemberService.deleteWorkspaceMember(authenticatedUser, workspaceId, id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/role")
    fun updateMemberRole(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @PathVariable id: UUID,
        @RequestBody dto: UpdateWorkspaceMemberRoleDto
    ): ResponseEntity<ApiResponse<WorkspaceMemberResponseDto>> {
        return ResponseEntity.ok(ApiResponse.ok(workspaceMemberService.updateMemberRole(authenticatedUser, workspaceId, id, dto)))
    }

    @PostMapping("/transfer-ownership")
    fun transferWorkspaceOwnership(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @RequestBody dto: TransferOwnershipDto
    ): ResponseEntity<ApiResponse<List<WorkspaceMemberResponseDto>>> {
        return ResponseEntity.ok(ApiResponse.ok(workspaceService.transferOwnership(authenticatedUser, workspaceId, dto)))
    }
}