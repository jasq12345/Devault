package com.john.devault

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DevaultApplication

fun main(args: Array<String>) {
    runApplication<DevaultApplication>(*args)
}
