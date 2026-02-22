package ${request.packageName}

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ${className}

fun main(args: Array<String>) {
    runApplication<${className}>(*args)
}
