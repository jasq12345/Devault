package dev.devault.auth.controller

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.dto.request.RefreshTokenDto
import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.dto.response.RegisterResponseDto
import dev.devault.auth.dto.response.TokenPair
import dev.devault.auth.service.AuthService
import dev.devault.commonlib.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
){

    @PostMapping("/register")
    fun register(@RequestBody @Valid dto: RegisterDto): ResponseEntity<ApiResponse<RegisterResponseDto>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(authService.register(dto)))
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid dto: LoginDto): ResponseEntity<ApiResponse<TokenPair>> {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(dto)))
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody dto: RefreshTokenDto): ResponseEntity<ApiResponse<TokenPair>> {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(dto)))
    }

    @PostMapping("/logout")
    fun logout(@RequestBody dto: RefreshTokenDto): ResponseEntity<Void> {
        authService.logout(dto)
        return ResponseEntity.noContent().build()
    }
}