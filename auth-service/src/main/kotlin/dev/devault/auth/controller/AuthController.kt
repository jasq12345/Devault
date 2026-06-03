package dev.devault.auth.controller

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.dto.request.RegisterDto
import dev.devault.auth.dto.response.LoginResponseDto
import dev.devault.auth.dto.response.RegisterResponseDto
import dev.devault.auth.model.User
import dev.devault.auth.service.AuthService
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
    fun register(@RequestBody dto: RegisterDto): ResponseEntity<RegisterResponseDto> {
        val user: User = authService.register(dto)
        val responseDto = RegisterResponseDto(
            id = user.id!!,
            email = user.email,
            username = user.username,
            role = user.authorities
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto)
    }

    @PostMapping("/login")
    fun login(@RequestBody dto: LoginDto): ResponseEntity<LoginResponseDto> {
        val token = authService.login(dto)

        return ResponseEntity.ok(LoginResponseDto(token))
    }
}