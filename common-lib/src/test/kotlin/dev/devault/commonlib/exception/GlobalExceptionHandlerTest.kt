package dev.devault.commonlib.exception

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest(classes = [GlobalExceptionHandlerTest.TestApplication::class])
class GlobalExceptionHandlerTest(@Autowired context: WebApplicationContext) {
    private val mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

    @Test
    fun `path variable that is not a valid UUID is a bad request`() {
        mockMvc.get("/items/:id").andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("Invalid value for parameter 'id'") }
        }
    }

    @Test
    fun `valid path variable reaches the controller`() {
        val id = UUID.randomUUID()

        mockMvc.get("/items/$id").andExpect {
            status { isOk() }
            jsonPath("$") { value(id.toString()) }
        }
    }

    @RestController
    class ItemController {
        @GetMapping("/items/{id}")
        fun findItemById(@PathVariable id: UUID) = id
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ItemController::class)
    class TestApplication
}
