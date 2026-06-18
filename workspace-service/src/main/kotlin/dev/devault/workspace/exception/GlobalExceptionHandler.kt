package dev.devault.workspace.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.security.access.AccessDeniedException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun errorResponse(message: String, status: HttpStatus) =
        ResponseEntity.status(status).body(mapOf("error" to message))

    @ExceptionHandler(ConflictException::class)
    fun handleConflicts(ex: ConflictException) =
        errorResponse(ex.message ?: "Unknown error", HttpStatus.CONFLICT)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException) =
        errorResponse(ex.message ?: "Not found", HttpStatus.NOT_FOUND)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException) =
        errorResponse(ex.message ?: "Access denied", HttpStatus.FORBIDDEN)

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