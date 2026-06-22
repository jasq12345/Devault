package dev.devault.commonlib.exception

import dev.devault.commonlib.response.ApiResponse
import dev.devault.commonlib.response.apiError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(ConflictException::class)
    fun handleConflicts(ex: ConflictException) =
        apiError(ex.message ?: "Unknown error", HttpStatus.CONFLICT)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException) =
        apiError(ex.message ?: "Not found", HttpStatus.NOT_FOUND)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException) =
        apiError(ex.message ?: "Access denied", HttpStatus.FORBIDDEN)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException) =
        apiError("Invalid credentials", HttpStatus.UNAUTHORIZED)

    @ExceptionHandler(InvalidOperationException::class)
    fun handleInvalidOperation(ex: InvalidOperationException) =
        apiError(ex.message ?: "Invalid operation", HttpStatus.BAD_REQUEST)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException) =
        apiError( "Invalid request", HttpStatus.BAD_REQUEST)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Internal error", ex)
        return apiError("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error", ex)
        return apiError("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}