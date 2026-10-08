package dev.devault.ingestion.exception

import dev.devault.commonlib.response.apiError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException

@RestControllerAdvice
class IngestionExceptionHandler {

    @ExceptionHandler(GithubApiException::class)
    fun handleGithubApiError(ex: GithubApiException) =
        apiError(ex.message ?: "GitHub API error", HttpStatus.BAD_GATEWAY)

    @ExceptionHandler(RestClientResponseException::class)
    fun handleGithubHttpError(ex: RestClientResponseException) =
        apiError("GitHub API error", HttpStatus.BAD_GATEWAY)
}