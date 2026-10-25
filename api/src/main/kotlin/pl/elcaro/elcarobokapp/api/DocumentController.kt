package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.DocumentService
import pl.elcaro.elcarobokapp.service.DocumentSummary
import pl.elcaro.elcarobokapp.service.DocumentView

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document retrieval")
class DocumentController(
    private val documentService: DocumentService,
    private val localeResolver: LocaleResolver,
) {
    @GetMapping
    @Operation(summary = "List all documents", description = "Returns a list of all documents with their IDs and names, localized to the request locale.")
    @ApiResponse(responseCode = "200", description = "Returns list of document summaries", content = [Content(array = ArraySchema(schema = Schema(implementation = DocumentSummary::class)))])
    fun getAllDocuments(request: HttpServletRequest): ResponseEntity<List<DocumentSummary>> =
        ResponseEntity.ok(documentService.getAllDocuments(localeResolver.resolveLocale(request)))

    @GetMapping("/{doc_id}")
    @Operation(summary = "Get document by ID", description = "Returns a single document with its full content, localized to the request locale.")
    @ApiResponse(responseCode = "200", description = "Returns the document with name and text content", content = [Content(schema = Schema(implementation = DocumentView::class))])
    @ApiResponse(responseCode = "404", description = "Document not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getDocumentById(
        @Parameter(description = "Document ID") @PathVariable("doc_id") id: String,
        request: HttpServletRequest,
    ): ResponseEntity<DocumentView> =
        ResponseEntity.ok(documentService.getDocumentById(id, localeResolver.resolveLocale(request)))
}
