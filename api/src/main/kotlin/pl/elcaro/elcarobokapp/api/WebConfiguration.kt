package pl.elcaro.elcarobokapp.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.ClientService

@Configuration
class WebConfiguration(
    private val clientService: ClientService,
) {
    @Bean
    fun localeResolver(): LocaleResolver = UserLanguageLocaleResolver(clientService)
}
