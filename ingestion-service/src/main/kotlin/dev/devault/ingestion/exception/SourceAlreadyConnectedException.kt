package dev.devault.ingestion.exception

import dev.devault.commonlib.exception.ConflictException

class SourceAlreadyConnectedException(message: String) : ConflictException(message)
