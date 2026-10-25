package pl.elcaro.elcarobokapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class ElcaroBokAppBeApplication

fun main(args: Array<String>) {
    runApplication<ElcaroBokAppBeApplication>(*args)
}