package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.*

@RestController
@RequestMapping("/api")
@Tag(name = "Support", description = "Support and FAQ API")
@SecurityRequirement(name = "Bearer")
class SupportController(
    private val supportService: SupportService,
    private val localeResolver: LocaleResolver
) {

    @GetMapping("/contact-help")
    @Operation(summary = "Get contact help data", description = "Returns contact information including phone and email with their schedules.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(schema = Schema(implementation = ContactHelpResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getContactHelp(request: HttpServletRequest): ResponseEntity<ContactHelpResponse> {
        val locale = localeResolver.resolveLocale(request)
        val response = supportService.getContactHelp(locale)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/faq/categories")
    @Operation(summary = "Get FAQ categories", description = "Returns a list of all available FAQ categories.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(array = ArraySchema(schema = Schema(implementation = FaqCategoryResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getFaqCategories(request: HttpServletRequest): ResponseEntity<List<FaqCategoryResponse>> {
        val locale = localeResolver.resolveLocale(request)
        val response = supportService.getFaqCategories(locale)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/faq/categories/{route}")
    @Operation(summary = "Get FAQ questions by category route", description = "Returns the FAQ page data including a list of questions for a specific category route.")
    @ApiResponse(responseCode = "200", description = "FAQ page data", content = [Content(schema = Schema(implementation = FaqPageResponse::class))])
    @ApiResponse(responseCode = "404", description = "Category not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getFaqCategoryByRoute(
        @Parameter(description = "The route identifier for the FAQ category", required = true)
        @PathVariable("route") route: String,
        request: HttpServletRequest
    ): ResponseEntity<FaqPageResponse> {
        val locale = localeResolver.resolveLocale(request)
        val response = supportService.getFaqCategoryByRoute(route, locale)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/faq/search")
    @Operation(summary = "Search FAQ questions", description = "Searches for FAQ questions based on a query string.")
    @ApiResponse(responseCode = "200", description = "Search results", content = [Content(array = ArraySchema(schema = Schema(implementation = FaqItem::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun searchFaq(
        @Parameter(description = "The search query string", required = true)
        @RequestParam("q") q: String,
        request: HttpServletRequest
    ): ResponseEntity<List<FaqItem>> {
        val locale = localeResolver.resolveLocale(request)
        val response = supportService.searchFaq(q, locale)
        return ResponseEntity.ok(response)
    }
}
