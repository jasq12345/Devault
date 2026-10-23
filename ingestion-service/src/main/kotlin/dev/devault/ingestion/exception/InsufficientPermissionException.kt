package dev.devault.ingestion.exception

import org.springframework.security.access.AccessDeniedException

class InsufficientPermissionException(message: String) : AccessDeniedException(message)