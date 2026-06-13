package dev.devault.auth.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun errorResponse(message: String, status: HttpStatus) =
        ResponseEntity.status(status).body(mapOf("error" to message))

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException) =
        errorResponse(ex.message ?: "Unknown error", HttpStatus.CONFLICT)

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException) =
        errorResponse(ex.message ?: "Unknown error", HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleNotFound(ex: UsernameNotFoundException) =
        errorResponse(ex.message ?: "Unknown error", HttpStatus.NOT_FOUND)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException) =
        errorResponse("Invalid credentials", HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException) =
        errorResponse("Invalid request", HttpStatus.BAD_REQUEST)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<Map<String, String>> {
        logger.error("Internal error", ex)
        return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<Map<String, String>> {
        logger.error("Unexpected error", ex)
        return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}