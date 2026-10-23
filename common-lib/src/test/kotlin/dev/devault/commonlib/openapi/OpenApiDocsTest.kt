package dev.devault.commonlib.openapi

import io.swagger.v3.oas.annotations.Operation
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    classes = [OpenApiDocsTest.TestApplication::class],
    properties = ["spring.application.name=test-service"]
)
class OpenApiDocsTest(@Autowired context: WebApplicationContext) {
    private val mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

    @Test
    fun `generated spec has service title and bearer scheme and hides @AuthenticationPrincipal parameters`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.info.title") { value("test-service") }
            jsonPath("$.components.securitySchemes.bearerAuth.scheme") { value("bearer") }
            jsonPath("$.security[0].bearerAuth") { exists() }
            jsonPath("$.paths['/greeting'].get.parameters[*].name") { value(contains("name")) }
        }
    }

    @Test
    fun `operations are tagged by controller name and summarized by method name unless set explicitly`() {
        mockMvc.get("/v3/api-docs").andExpect {
            jsonPath("$.paths['/greeting'].get.tags") { value(contains("greeting")) }
            jsonPath("$.tags[*].name") { value(contains("greeting")) }
            jsonPath("$.paths['/greeting'].get.summary") { value("find greeting by name") }
            jsonPath("$.paths['/greeting/custom'].get.summary") { value("Custom summary") }
        }
    }

    data class TestPrincipal(val id: String)

    @RestController
    class GreetingController {
        @GetMapping("/greeting")
        fun findGreetingByName(@AuthenticationPrincipal principal: TestPrincipal?, @RequestParam name: String) =
            "Hello $name"

        @Operation(summary = "Custom summary")
        @GetMapping("/greeting/custom")
        fun customGreeting() = "Hello"
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(GreetingController::class)
    class TestApplication
}
