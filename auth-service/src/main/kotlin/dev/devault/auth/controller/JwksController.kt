package dev.devault.auth.controller

import dev.devault.auth.service.JwksService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JwksController(
    private val jwksService: JwksService
) {
    @GetMapping("/.well-known/jwks.json")
    fun jwks(): Map<String, Any> {
        return jwksService.getJwks()
    }
}