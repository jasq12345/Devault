package dev.devault.ingestion.client.workspace

import dev.devault.authlib.security.principal.AuthenticatedUser
import dev.devault.ingestion.exception.WorkspaceServiceException
import dev.devault.commonlib.type.WorkspaceRole
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.RestClient
import java.io.IOException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * No real workspace-service call is made: MockRestServiceServer replaces the HTTP layer of the RestClient.
 * The user's token is put into the security context the same way JwtAuthenticationProvider does it.
 */
class WorkspaceClientTest {
    private val restClientBuilder = RestClient.builder().baseUrl("http://workspace.test/api/v1")
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val client = WorkspaceClient(restClientBuilder.build())

    private val workspaceId = UUID.randomUUID()
    private val jwt = "test-jwt-not-a-real-token"
    private val memberUrl = "http://workspace.test/api/v1/workspaces/$workspaceId/members/me"

    @BeforeEach
    fun signIn() {
        val principal = AuthenticatedUser(UUID.randomUUID(), "testuser", listOf())
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(principal, jwt, emptyList())
    }

    @AfterEach
    fun signOut() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `asks for the caller's own membership with the caller's token`() {
        server.expect(requestTo(memberUrl))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer $jwt"))
            .andRespond(withSuccess(memberResponse("OWNER"), MediaType.APPLICATION_JSON))

        client.getWorkspaceMemberRole(workspaceId)

        server.verify()
    }

    // The response also carries id, userId and workspaceId; the client reads the role only.
    @ParameterizedTest
    @EnumSource(WorkspaceRole::class)
    fun `returns the role workspace-service reports`(role: WorkspaceRole) {
        respondWith(memberResponse(role.name))

        assertEquals(role, client.getWorkspaceMemberRole(workspaceId))
    }

    @Test
    fun `turns 403 into access denied`() {
        server.expect(requestTo(memberUrl)).andRespond(
            withForbiddenRequest().contentType(MediaType.APPLICATION_JSON).body(errorResponse("Access denied"))
        )

        assertThrows<AccessDeniedException> {
            client.getWorkspaceMemberRole(workspaceId)
        }
    }

    @Test
    fun `turns 404 into not found`() {
        server.expect(requestTo(memberUrl)).andRespond(
            withResourceNotFound().contentType(MediaType.APPLICATION_JSON).body(errorResponse("Workspace not found"))
        )

        val exception = assertThrows<NoSuchElementException> {
            client.getWorkspaceMemberRole(workspaceId)
        }

        assertEquals("Workspace not found", exception.message)
    }

    @Test
    fun `reports a server error of workspace-service as WorkspaceServiceException`() {
        server.expect(requestTo(memberUrl)).andRespond(withServerError())

        assertThrows<WorkspaceServiceException> {
            client.getWorkspaceMemberRole(workspaceId)
        }
    }

    @Test
    fun `reports any other refusal as WorkspaceServiceException`() {
        server.expect(requestTo(memberUrl)).andRespond(withUnauthorizedRequest())

        assertThrows<WorkspaceServiceException> {
            client.getWorkspaceMemberRole(workspaceId)
        }
    }

    @Test
    fun `reports an unreachable workspace-service as WorkspaceServiceException`() {
        server.expect(requestTo(memberUrl)).andRespond(withException(IOException("connection refused")))

        assertThrows<WorkspaceServiceException> {
            client.getWorkspaceMemberRole(workspaceId)
        }
    }

    @Test
    fun `refuses a role it does not know instead of guessing`() {
        respondWith(memberResponse("AUDITOR"))

        val exception = assertThrows<WorkspaceServiceException> {
            client.getWorkspaceMemberRole(workspaceId)
        }

        assertEquals("Workspace service returned an unknown role: AUDITOR", exception.message)
    }

    @Test
    fun `refuses a response without a member`() {
        respondWith("""{ "success": true, "data": null, "error": null, "code": null }""")

        assertThrows<WorkspaceServiceException> {
            client.getWorkspaceMemberRole(workspaceId)
        }
    }

    @Test
    fun `fails without calling workspace-service when there is no user token`() {
        SecurityContextHolder.clearContext()

        assertThrows<IllegalStateException> {
            client.getWorkspaceMemberRole(workspaceId)
        }

        // No expectation was registered, so any request would have failed the test already.
        server.verify()
    }

    @Test
    fun `does not put the token into the error of a failed call`() {
        server.expect(requestTo(memberUrl)).andRespond(withServerError())

        val exception = assertThrows<WorkspaceServiceException> {
            client.getWorkspaceMemberRole(workspaceId)
        }

        assertFalse(exception.message.orEmpty().contains(jwt))
        assertFalse(exception.cause?.message.orEmpty().contains(jwt))
    }

    private fun respondWith(json: String) {
        server.expect(requestTo(memberUrl)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
    }

    // The shape workspace-service really sends: ApiResponse around WorkspaceMemberResponseDto.
    private fun memberResponse(role: String) = """
        {
          "success": true,
          "data": {
            "id": "${UUID.randomUUID()}",
            "userId": "${UUID.randomUUID()}",
            "role": "$role",
            "workspaceId": "$workspaceId"
          },
          "error": null,
          "code": null
        }
    """

    private fun errorResponse(message: String) =
        """{ "success": false, "data": null, "error": "$message", "code": null }"""
}
