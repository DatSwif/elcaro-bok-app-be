package pl.elcaro.elcarobokapp.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.postgresql.PostgreSQLContainer
import pl.elcaro.elcarobokapp.TestApplication

@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("security-test")
abstract class BaseIntegrationTest(body: DescribeSpec.() -> Unit = {}) : DescribeSpec() {
    companion object {
        private val postgres = PostgreSQLContainer("postgres:17-alpine").apply {
            start()
            System.setProperty("spring.datasource.url", jdbcUrl)
            System.setProperty("spring.datasource.username", username)
            System.setProperty("spring.datasource.password", password)
            System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver")
        }
    }

    init {
        extension(SpringExtension())
        body()
    }
}
