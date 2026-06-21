package dev.devault.authlib.security.entrypoint

import com.fasterxml.jackson.databind.ObjectMapper
import dev.devault.commonlib.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class AuthLibAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.outputStream.write(
            objectMapper.writeValueAsBytes(ApiResponse.error(authException.message ?: "Invalid or expired token"))
        )
    }
}