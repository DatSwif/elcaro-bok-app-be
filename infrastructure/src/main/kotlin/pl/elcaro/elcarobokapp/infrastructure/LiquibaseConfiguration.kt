package pl.elcaro.elcarobokapp.infrastructure

import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class LiquibaseConfiguration(
    @Value($$"${spring.liquibase.contexts:}") private val liquibaseContexts: String
) {
    @Bean
    fun liquibase(dataSource: DataSource): SpringLiquibase {
        return SpringLiquibase().apply {
            this.dataSource = dataSource
            changeLog = "classpath:/db/changelog/db.changelog-master.xml"
            this.contexts = liquibaseContexts.ifBlank { null }
        }
    }
}
