package dev.devault.ingestion.exception

import dev.devault.commonlib.response.apiError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class IngestionExceptionHandler {

    @ExceptionHandler(GitHubApiException::class)
    fun handleGitHubApiError(ex: GitHubApiException) =
        apiError(ex.message ?: "GitHub API error", HttpStatus.BAD_GATEWAY)
}