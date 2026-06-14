package dev.devault.workspace.controller

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.workspace.model.Workspace
import dev.devault.workspace.service.WorkspaceService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workspaces")
class WorkspaceController(
    private val workspaceService: WorkspaceService
) {

    @GetMapping("")
    fun findAllWorkspaces(@AuthenticationPrincipal authenticatedUser: AuthenticatedUser): ResponseEntity<MutableList<Workspace>> {
        return ResponseEntity.ok(workspaceService.findAllWorkspaces(authenticatedUser))
    }

    @GetMapping("/{id}")
    fun findWorkspaceById(@AuthenticationPrincipal authenticatedUser: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<Workspace> {
        return ResponseEntity.ok(workspaceService.findWorkspaceById(authenticatedUser, id))
    }

    @PostMapping("")
    fun saveWorkspace(@AuthenticationPrincipal authenticatedUser: AuthenticatedUser, @RequestBody dto: ): ResponseEntity<> {

    }
}