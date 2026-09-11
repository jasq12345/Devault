package dev.devault.ingestion.controller

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.commonlib.response.ApiResponse
import dev.devault.ingestion.dto.request.CredentialRequestDto
import dev.devault.ingestion.dto.response.CredentialResponseDto
import dev.devault.ingestion.service.CredentialService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/credentials")
class CredentialController(
    private val credentialService: CredentialService
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
}