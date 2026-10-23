package dev.devault.authlib.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    classes = [AuthLibSecurityFilterChainTest.TestApplication::class],
    properties = [
        "authlib.jwks.uri=http://localhost/.well-known/jwks.json",
        "authlib.jwt.issuer=test-issuer",
        "authlib.security.public-paths=/public/**",
    ]
)
class AuthLibSecurityFilterChainTest(@Autowired context: WebApplicationContext) {
    private val mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    @Test
    fun `configured public paths are reachable without a token`() {
        mockMvc.get("/public/ping").andExpect { status { isOk() } }
    }

    @Test
    fun `other paths still require a token`() {
        mockMvc.get("/private/ping").andExpect { status { isUnauthorized() } }
    }

    @RestController
    class PingController {
        @GetMapping("/public/ping", "/private/ping")
        fun ping() = "pong"
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PingController::class)
    class TestApplication
}
