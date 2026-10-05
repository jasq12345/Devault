package dev.devault.ingestion.dto.request

import jakarta.validation.Validation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.assertEquals

class SaveIngestionSourceDtoTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    private fun invalidFields(owner: String = "octo", name: String = "repo"): Set<String> =
        validator.validate(SaveIngestionSourceDto(owner, name, UUID.randomUUID()))
            .map { it.propertyPath.toString() }
            .toSet()

    @Nested
    inner class Owner {
        @ParameterizedTest
        @ValueSource(strings = ["octo", "spring-projects", "a", "A1-b2", "Octo-Cat-9"])
        fun `accepts GitHub owner names`(owner: String) {
            assertEquals(emptySet(), invalidFields(owner = owner))
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "-octo", "octo-", "oc--to", "octo/evil", "oc to", "octo_cat", "octo.cat", "żółw"])
        fun `rejects names GitHub would not allow`(owner: String) {
            assertEquals(setOf("owner"), invalidFields(owner = owner))
        }

        @Test
        fun `accepts 39 characters and rejects 40`() {
            assertEquals(emptySet(), invalidFields(owner = "a".repeat(39)))
            assertEquals(setOf("owner"), invalidFields(owner = "a".repeat(40)))
        }
    }

    @Nested
    inner class Name {
        @ParameterizedTest
        @ValueSource(strings = ["repo", "spring-boot", ".github", "my_repo", "socket.io", "a--b", "Repo-1"])
        fun `accepts GitHub repository names`(name: String) {
            assertEquals(emptySet(), invalidFields(name = name))
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "re po", "repo/evil", "repo#1", "repo?x=1", "żółw"])
        fun `rejects names GitHub would not allow`(name: String) {
            assertEquals(setOf("name"), invalidFields(name = name))
        }

        @Test
        fun `accepts 100 characters and rejects 101`() {
            assertEquals(emptySet(), invalidFields(name = "a".repeat(100)))
            assertEquals(setOf("name"), invalidFields(name = "a".repeat(101)))
        }
    }

    @Test
    fun `reports both fields when both are invalid`() {
        assertEquals(setOf("owner", "name"), invalidFields(owner = "octo/evil", name = "re po"))
    }

    @Test
    fun `violation carries a readable message instead of the raw pattern`() {
        val violations = validator.validate(SaveIngestionSourceDto("octo/evil", "re po", UUID.randomUUID()))

        assertEquals(
            setOf(
                "Owner can only contain letters, digits and single hyphens, and cannot start or end with a hyphen",
                "Name can only contain letters, digits, dots, hyphens and underscores"
            ),
            violations.map { it.message }.toSet()
        )
    }
}
