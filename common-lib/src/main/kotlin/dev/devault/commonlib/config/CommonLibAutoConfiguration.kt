package dev.devault.commonlib.config

import dev.devault.commonlib.exception.GlobalExceptionHandler
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(GlobalExceptionHandler::class)
class CommonLibAutoConfiguration