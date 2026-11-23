package se.exempel.avstamning

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AvstamningsApplication

fun main(args: Array<String>) {
    runApplication<AvstamningsApplication>(*args)
}
