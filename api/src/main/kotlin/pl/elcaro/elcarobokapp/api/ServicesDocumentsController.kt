package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
@RequestMapping("/api/services")
@Tag(name = "Services Documents", description = "Services document retrieval")
@SecurityRequirement(name = "Bearer")
class ServicesDocumentsController(
    private val authDocumentService: AuthDocumentService,
    private val localeResolver: LocaleResolver,
) {
    @GetMapping("/promotions-regulations")
    @Operation(summary = "Get promotion regulations document", description = "Returns the promotion regulations document for package purchase confirmation.")
    @ApiResponse(responseCode = "200", description = "Promotion regulations document", content = [Content(schema = Schema(implementation = DocumentContentResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getPromotionsRegulations(request: HttpServletRequest): ResponseEntity<DocumentContentResponse> =
        ResponseEntity.ok(authDocumentService.getPromotionsRegulations(localeResolver.resolveLocale(request)))
}
