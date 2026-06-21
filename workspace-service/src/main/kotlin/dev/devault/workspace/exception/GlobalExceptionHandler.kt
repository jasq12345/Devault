package dev.devault.workspace.exception

import dev.devault.commonlib.response.apiError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CannotModifyOwnerException::class)
    fun handleCannotModifyOwner(ex: CannotModifyOwnerException) =
        apiError(ex.message ?: "Cannot modify owner", HttpStatus.FORBIDDEN)
}