package dev.devault.workspace.exception

import dev.devault.commonlib.exception.ConflictException

class SlugAlreadyExistsException(message: String) : ConflictException(message)