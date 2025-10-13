package com.smartpacking.ai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AiWorkerApplication

fun main(args: Array<String>) {
	runApplication<AiWorkerApplication>(*args)
}
