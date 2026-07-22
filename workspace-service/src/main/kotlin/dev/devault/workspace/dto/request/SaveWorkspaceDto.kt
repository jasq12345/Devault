package dev.devault.workspace.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SaveWorkspaceDto(
    @NotBlank(message = "Name must not be blank")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    val name: String,

    @NotBlank(message = "Slug must not be blank")
    @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
    @Pattern(
        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
        message = "Slug can only contain lowercase letters, digits and hyphens"
    )
    val slug: String,
)