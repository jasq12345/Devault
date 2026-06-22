package dev.devault.auth.exception

import dev.devault.commonlib.response.apiError
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUsernameNotFound(ex: UsernameNotFoundException) =
        apiError("Invalid credentials", HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException) =
        apiError(ex.message ?: "Unknown error", HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(io.jsonwebtoken.JwtException::class)
    fun handleJwtException(ex: io.jsonwebtoken.JwtException) =
        apiError( "Invalid or expired token", HttpStatus.UNAUTHORIZED)
}