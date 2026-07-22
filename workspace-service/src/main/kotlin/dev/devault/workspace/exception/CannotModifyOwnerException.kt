package dev.devault.workspace.exception

import org.springframework.security.access.AccessDeniedException

class CannotModifyOwnerException(message: String) : AccessDeniedException(message)