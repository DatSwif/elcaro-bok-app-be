package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.ConsentService
import pl.elcaro.elcarobokapp.service.ConsentUpdateRequest
import pl.elcaro.elcarobokapp.service.MarketingConsentResponse

@RestController
@RequestMapping("/api/clients/consents")
@Tag(name = "Consents", description = "Client consent management")
class ConsentController(
    private val consentService: ConsentService,
    private val localeResolver: LocaleResolver,
) {

    @GetMapping
    @Operation(summary = "Get consents", description = "Returns all consent types with the current client's granted/not-granted status.")
    @ApiResponse(responseCode = "200", description = "Returns list of consents per type", content = [Content(array = ArraySchema(schema = Schema(implementation = MarketingConsentResponse::class)))])
    @ApiResponse(responseCode = "404", description = "Client not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getConsents(request: HttpServletRequest): ResponseEntity<List<MarketingConsentResponse>> =
        ResponseEntity.ok(
            consentService.getClientConsents(
                authenticatedClientNumber(),
                localeResolver.resolveLocale(request),
            )
        )

    @PutMapping
    @Operation(summary = "Update consents", description = "Bulk-updates consent choices for the current client.")
    @ApiResponse(responseCode = "200", description = "Returns the updated list of consents", content = [Content(array = ArraySchema(schema = Schema(implementation = MarketingConsentResponse::class)))])
    @ApiResponse(responseCode = "404", description = "Client or consent type not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun updateConsents(
        @Valid @RequestBody requests: List<@Valid ConsentUpdateRequest>,
        request: HttpServletRequest,
    ): ResponseEntity<List<MarketingConsentResponse>> {
        val locale = localeResolver.resolveLocale(request)
        val clientNumber = authenticatedClientNumber()
        requests.forEach { consentService.updateConsent(clientNumber, it) }
        return ResponseEntity.ok(consentService.getClientConsents(clientNumber, locale))
    }
}
