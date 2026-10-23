package dev.devault.ingestion.exception

import dev.devault.commonlib.exception.ConflictException

class SourceSyncInProgressException(message: String) : ConflictException(message)
