package dev.devault.workspace.exception

import dev.devault.commonlib.exception.ConflictException

class UserAlreadyMemberException(message: String) : ConflictException(message)