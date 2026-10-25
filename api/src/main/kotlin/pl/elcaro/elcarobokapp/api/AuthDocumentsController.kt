package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.AuthDocumentService
import pl.elcaro.elcarobokapp.service.DocumentContentResponse

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Documents", description = "Registration flow documents")
class AuthDocumentsController(
    private val authDocumentService: AuthDocumentService,
    private val localeResolver: LocaleResolver,
) {
    @GetMapping("/terms-of-service")
    @Operation(summary = "Get the registration terms of service document", description = "Returns the terms of service document for the registration flow.")
    @ApiResponse(responseCode = "200", description = "Terms of service document", content = [Content(schema = Schema(implementation = DocumentContentResponse::class))])
    fun getTermsOfService(request: HttpServletRequest): ResponseEntity<DocumentContentResponse> =
        ResponseEntity.ok(authDocumentService.getTermsOfService(localeResolver.resolveLocale(request)))

    @GetMapping("/privacy-policy")
    @Operation(summary = "Get the registration privacy policy document", description = "Returns the privacy policy document for the registration flow.")
    @ApiResponse(responseCode = "200", description = "Privacy policy document", content = [Content(schema = Schema(implementation = DocumentContentResponse::class))])
    fun getPrivacyPolicy(request: HttpServletRequest): ResponseEntity<DocumentContentResponse> =
        ResponseEntity.ok(authDocumentService.getPrivacyPolicy(localeResolver.resolveLocale(request)))

    @GetMapping("/marketing-consents")
    @Operation(summary = "Get the registration marketing consents document", description = "Returns the marketing consents document for the registration flow.")
    @ApiResponse(responseCode = "200", description = "Marketing consents document", content = [Content(schema = Schema(implementation = DocumentContentResponse::class))])
    fun getMarketingConsents(request: HttpServletRequest): ResponseEntity<DocumentContentResponse> =
        ResponseEntity.ok(authDocumentService.getMarketingConsents(localeResolver.resolveLocale(request)))
}
