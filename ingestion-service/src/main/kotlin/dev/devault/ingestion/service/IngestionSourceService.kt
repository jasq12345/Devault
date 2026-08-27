package dev.devault.ingestion.service

import dev.devault.ingestion.repository.IngestionSourceRepository
import org.springframework.stereotype.Service

@Service
class IngestionSourceService(
    private val repository: IngestionSourceRepository,
    private val backfillService: BackfillService
) {

    fun connectSource() {}

}