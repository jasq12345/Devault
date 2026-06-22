package dev.devault.workspace.dto.request

import java.util.UUID

data class TransferOwnershipDto(
    val newOwnerId: UUID
)
