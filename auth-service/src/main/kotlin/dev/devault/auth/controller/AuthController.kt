package dev.devault.auth.controller

import dev.devault.auth.dto.request.LoginDto
import dev.devault.auth.model.User
import dev.devault.auth.service.AuthService
import org.springframework.web.bind.annotation.GetMapping
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
    fun register(@RequestBody user: User): User {
        return authService.register(user)
    }

    @GetMapping("/login")
    fun login(@RequestBody dto: LoginDto): String{
        return authService.login(dto)
    }
}