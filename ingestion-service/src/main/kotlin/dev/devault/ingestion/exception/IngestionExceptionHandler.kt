package dev.devault.ingestion.exception

import dev.devault.commonlib.response.ApiResponse
import dev.devault.commonlib.response.apiError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException

@RestControllerAdvice
class IngestionExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(GithubApiException::class)
    fun handleGithubApiError(ex: GithubApiException) =
        apiError(ex.message ?: "GitHub API error", HttpStatus.BAD_GATEWAY)

    @ExceptionHandler(RestClientResponseException::class)
    fun handleGithubHttpError(ex: RestClientResponseException) =
        apiError("GitHub API error", HttpStatus.BAD_GATEWAY)

    @ExceptionHandler(WorkspaceServiceException::class)
    fun handleWorkspaceServiceError(ex: WorkspaceServiceException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Workspace service call failed", ex)
        return apiError("Workspace service error", HttpStatus.BAD_GATEWAY)
    }
}