package dev.devault.auth.exception

import dev.devault.commonlib.exception.ConflictException

class UserAlreadyExistsException(message: String) : ConflictException(message)