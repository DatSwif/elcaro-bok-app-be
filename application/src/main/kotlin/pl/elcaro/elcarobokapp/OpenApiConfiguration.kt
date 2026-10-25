package pl.elcaro.elcarobokapp

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun modelResolver(objectMapper: ObjectMapper): ModelResolver = ModelResolver(objectMapper)

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(Info().title("Elcaro BOK API").version("1.0"))
        .addSecurityItem(SecurityRequirement().addList("Bearer"))
        .components(
            Components().addSecuritySchemes(
                "Bearer",
                SecurityScheme()
                    .name("Authorization")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"),
            ),
        )
}
