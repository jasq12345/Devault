package dev.devault.ingestion.controller

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.response.ApiResponse
import dev.devault.ingestion.dto.request.SaveIngestionSourceDto
import dev.devault.ingestion.dto.response.IngestionSourceResponseDto
import dev.devault.ingestion.service.IngestionSourceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/workspaces/{workspaceId}/ingestion-sources")
class IngestionSourceController(
    private val service: IngestionSourceService
) {
    @PostMapping
    fun connectSource(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @Valid @RequestBody dto: SaveIngestionSourceDto
    ): ResponseEntity<ApiResponse<IngestionSourceResponseDto>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(service.connectSource(authenticatedUser, workspaceId, dto)))
    }

    @GetMapping
    fun findAllIngestedSources(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID
    ): ResponseEntity<ApiResponse<List<IngestionSourceResponseDto>>> {
        return ResponseEntity.ok(ApiResponse.ok(service.findAllIngestedSources(authenticatedUser, workspaceId)))
    }

    @GetMapping("/{sourceId}")
    fun findIngestedSource(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @PathVariable sourceId: UUID
    ): ResponseEntity<ApiResponse<IngestionSourceResponseDto>> {
        return ResponseEntity.ok(ApiResponse.ok(service.findIngestedSource(authenticatedUser, workspaceId, sourceId)))
    }

    @DeleteMapping("/{sourceId}")
    fun deleteIngestedSource(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable workspaceId: UUID,
        @PathVariable sourceId: UUID
    ): ResponseEntity<Void> {
        service.deleteIngestedSource(authenticatedUser, workspaceId, sourceId)
        return ResponseEntity.noContent().build()
    }
}