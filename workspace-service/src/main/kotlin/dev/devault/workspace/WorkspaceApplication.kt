package dev.devault.workspace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WorkspaceApplication

fun main(args: Array<String>) {
    runApplication<WorkspaceApplication>(*args)
}
