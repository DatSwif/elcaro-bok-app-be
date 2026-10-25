package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.elcaro.elcarobokapp.service.DocumentContentResponse
import pl.elcaro.elcarobokapp.service.PdfGeneratorService
import pl.elcaro.elcarobokapp.service.DocumentService
import java.util.Locale

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Content", description = "Endpoints to retrieve raw document content")
class DocumentContentController(
    private val pdfGeneratorService: PdfGeneratorService,
    private val documentService: DocumentService
) {

    @GetMapping("/{id}/text")
    @Operation(summary = "Get document HTML content", description = "Returns the full HTML body of a document identified by its unique ID for on-demand loading.")
    @ApiResponse(responseCode = "200", description = "HTML content of the document", content = [Content(schema = Schema(implementation = DocumentContentResponse::class))])
    fun getDocumentText(
        @Parameter(description = "Document ID") @PathVariable("id") id: String,
        locale: Locale
    ): ResponseEntity<DocumentContentResponse> {
        val documentView = documentService.getDocumentById(id, locale)
        val response = DocumentContentResponse(
            id = documentView.id,
            title = documentView.name,
            text = documentView.text,
            type = null
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(summary = "Download document as PDF", description = "Generates and returns a PDF version of the requested document.")
    @ApiResponse(responseCode = "200", description = "The document file in PDF format", content = [Content(mediaType = "application/pdf", schema = Schema(type = "string", format = "binary"))])
    fun downloadDocumentPdf(
        @Parameter(description = "Document ID") @PathVariable("id") id: String,
        locale: Locale
    ): ResponseEntity<ByteArrayResource> {
        val documentView = documentService.getDocumentById(id, locale)
        val pdfBytes = pdfGeneratorService.generateDocumentPdf(documentView.name, documentView.text)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document-$id.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfBytes.size.toLong())
            .body(ByteArrayResource(pdfBytes))
    }
}
