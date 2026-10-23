package dev.devault.ingestion.controller

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.response.ApiResponse
import dev.devault.ingestion.dto.request.CredentialRequestDto
import dev.devault.ingestion.dto.response.CredentialResponseDto
import dev.devault.ingestion.dto.response.RateLimitResponseDto
import dev.devault.ingestion.service.CredentialDeletionService
import dev.devault.ingestion.service.CredentialService
import dev.devault.ingestion.service.RateLimitService
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
@RequestMapping("/credentials")
class CredentialController(
    private val credentialService: CredentialService,
    private val credentialDeletionService: CredentialDeletionService,
    private val rateLimitService: RateLimitService
){
    @GetMapping
    fun findAllCredentials(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser
    ): ResponseEntity<ApiResponse<List<CredentialResponseDto>>> {
        return ResponseEntity.ok(ApiResponse.ok(credentialService.findAllForUser(authenticatedUser.id)))
    }

    @PostMapping
    fun saveCredential(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Valid @RequestBody dto: CredentialRequestDto
    ): ResponseEntity<ApiResponse<CredentialResponseDto>> {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(credentialService.save(dto, authenticatedUser.id)))
    }

    @DeleteMapping("/{id}")
    fun deleteCredential(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        credentialDeletionService.delete(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/rate-limit")
    fun findRateLimit(
        @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<RateLimitResponseDto>> {
        return ResponseEntity.ok(ApiResponse.ok(rateLimitService.findForCredential(id, authenticatedUser.id)))
    }
}