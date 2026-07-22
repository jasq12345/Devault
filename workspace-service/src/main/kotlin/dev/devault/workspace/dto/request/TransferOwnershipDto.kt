package dev.devault.workspace.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class TransferOwnershipDto(
    @NotNull(message = "newOwnerId must not be null")
    var newOwnerId: UUID
)