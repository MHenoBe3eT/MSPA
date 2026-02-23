package mspa

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["mspa", "config", "controller", "auth", "user", "entity", "repository", "transformer", "storage", "file", "ai"])
@EntityScan(basePackages = ["entity"])
@EnableJpaRepositories(basePackages = ["repository"])
class MSPAApplication

fun main(args: Array<String>) {
    runApplication<MSPAApplication>(*args)
}