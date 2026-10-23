package dev.devault.ingestion.exception

import dev.devault.commonlib.exception.ConflictException

class CredentialInUseException(message: String) : ConflictException(message)
