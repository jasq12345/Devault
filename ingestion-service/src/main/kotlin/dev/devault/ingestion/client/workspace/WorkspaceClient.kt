package dev.devault.ingestion.client.workspace

import dev.devault.commonlib.response.ApiResponse
import dev.devault.ingestion.client.workspace.dto.WorkspaceMemberResponse
import dev.devault.ingestion.exception.WorkspaceServiceException
import dev.devault.commonlib.type.WorkspaceRole
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

@Component
class WorkspaceClient(
    @Qualifier("workspaceRestClient") private val restClient: RestClient,
) {

    // Asks workspace-service with the caller's own token, so it answers exactly as it would answer the caller.
    fun getWorkspaceMemberRole(workspaceId: UUID): WorkspaceRole {
        val token = currentUserToken()

        val response = try {
            restClient.get()
                .uri("/workspaces/{workspaceId}/members/me", workspaceId)
                .header("Authorization", "Bearer $token")
                .retrieve()
                .body(object : ParameterizedTypeReference<ApiResponse<WorkspaceMemberResponse>>() {})
        } catch (_: HttpClientErrorException.Forbidden) {
            // workspace-service has decided: the caller is not a member.
            throw AccessDeniedException("Access denied")
        } catch (_: HttpClientErrorException.NotFound) {
            throw NoSuchElementException("Workspace not found")
        } catch (ex: RestClientException) {
            throw WorkspaceServiceException("Workspace service request failed", ex)
        }

        val role = response?.data?.role
            ?: throw WorkspaceServiceException("Workspace service returned no member")

        // An unknown role is refused instead of guessed, so a new role never gets rights by accident.
        return WorkspaceRole.entries.find { it.name == role }
            ?: throw WorkspaceServiceException("Workspace service returned an unknown role: $role")
    }

    // The caller's JWT, kept as credentials by JwtAuthenticationProvider. It exists only on the request thread,
    // so this fails in @Async code such as the backfill.
    private fun currentUserToken(): String {
        return SecurityContextHolder.getContext().authentication?.credentials as? String
            ?: throw IllegalStateException("No user token in the security context")
    }
}
