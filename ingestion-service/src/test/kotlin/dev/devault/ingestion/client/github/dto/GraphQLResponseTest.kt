package dev.devault.ingestion.client.github.dto

import dev.devault.ingestion.exception.GithubApiException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class GraphQLResponseTest {

    @Test
    fun `returns data when there are no errors`() {
        val response = GraphQLResponse(data = "payload")

        assertEquals("payload", response.unwrap())
    }

    @Test
    fun `returns data when the errors list is empty`() {
        val response = GraphQLResponse(data = "payload", errors = emptyList())

        assertEquals("payload", response.unwrap())
    }

    @Test
    fun `throws with all error messages joined`() {
        val response = GraphQLResponse<String>(
            errors = listOf(GraphQLError("first problem"), GraphQLError("second problem"))
        )

        val exception = assertThrows<GithubApiException> { response.unwrap() }

        assertEquals("first problem; second problem", exception.message)
    }

    @Test
    fun `throws when errors come together with partial data`() {
        val response = GraphQLResponse(data = "partial", errors = listOf(GraphQLError("problem")))

        assertThrows<GithubApiException> { response.unwrap() }
    }

    @Test
    fun `throws when there is neither data nor errors`() {
        val response = GraphQLResponse<String>()

        val exception = assertThrows<GithubApiException> { response.unwrap() }

        assertEquals("GraphQL response missing data", exception.message)
    }
}
