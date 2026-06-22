package dev.devault.commonlib.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun apiError(message: String, status: HttpStatus, code: String? = null): ResponseEntity<ApiResponse<Nothing>> =
    ResponseEntity.status(status).body(ApiResponse.error(message, code))