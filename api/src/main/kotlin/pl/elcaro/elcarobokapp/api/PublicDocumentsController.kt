package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
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
import pl.elcaro.elcarobokapp.service.DocumentGroupResponse
import pl.elcaro.elcarobokapp.service.GroupedDocumentService

@RestController
@RequestMapping("/api")
@Tag(name = "Public Documents", description = "Public grouped document retrieval")
class PublicDocumentsController(
    private val groupedDocumentService: GroupedDocumentService,
    private val localeResolver: LocaleResolver,
) {
    @GetMapping("/privacy-policy")
    @Operation(summary = "Get grouped Privacy Policy documents", description = "Retrieves all privacy-related documents and marketing consents, grouped by their respective categories.")
    @ApiResponse(responseCode = "200", description = "Successful retrieval of privacy policy documents", content = [Content(array = ArraySchema(schema = Schema(implementation = DocumentGroupResponse::class)))])
    fun getPrivacyPolicy(request: HttpServletRequest): ResponseEntity<List<DocumentGroupResponse>> =
        ResponseEntity.ok(groupedDocumentService.getPrivacyPolicy(localeResolver.resolveLocale(request)))

    @GetMapping("/terms-of-service")
    @Operation(summary = "Get grouped Terms of Service documents", description = "Returns a hierarchical list of document sections and their metadata (ID, title, type) without the heavy HTML content.")
    @ApiResponse(responseCode = "200", description = "A list of document sections with nested document metadata", content = [Content(array = ArraySchema(schema = Schema(implementation = DocumentGroupResponse::class)))])
    fun getTermsOfService(request: HttpServletRequest): ResponseEntity<List<DocumentGroupResponse>> =
        ResponseEntity.ok(groupedDocumentService.getTermsOfService(localeResolver.resolveLocale(request)))
}
