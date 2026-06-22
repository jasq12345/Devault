package dev.devault.workspace.exception

import dev.devault.commonlib.exception.ConflictException

class CannotRemoveProtectedRoleException(message: String) : ConflictException(message)