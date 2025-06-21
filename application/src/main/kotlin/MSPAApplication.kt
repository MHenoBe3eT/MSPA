package mspa

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MSPAApplication

fun main(args: Array<String>) {
    runApplication<MSPAApplication>(*args)
}