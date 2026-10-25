package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "Account settings and consents management")
@SecurityRequirement(name = "Bearer")
class SettingsController(
    private val consentService: ConsentService,
    private val localeResolver: LocaleResolver
) {

    @GetMapping("/consents")
    @Operation(summary = "Get all client consents", description = "Returns a list of all marketing and legal consents for the authenticated client, localized to the request locale.")
    @ApiResponse(responseCode = "200", description = "Returns list of consent statuses", content = [Content(array = ArraySchema(schema = Schema(implementation = MarketingConsentResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getConsents(request: HttpServletRequest): ResponseEntity<List<MarketingConsentResponse>> {
        val locale = localeResolver.resolveLocale(request)
        val consents = consentService.getClientConsents(authenticatedClientNumber(), locale)
        return ResponseEntity.ok(consents)
    }

    @PutMapping("/consents")
    @Operation(summary = "Update a consent status", description = "Updates the granted status of a specific consent for the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Consent successfully updated")
    @ApiResponse(responseCode = "400", description = "Validation error", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Consent type not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun updateConsent(@Valid @RequestBody request: ConsentUpdateRequest): ResponseEntity<Void> {
        consentService.updateConsent(authenticatedClientNumber(), request)
        return ResponseEntity.ok().build()
    }
}