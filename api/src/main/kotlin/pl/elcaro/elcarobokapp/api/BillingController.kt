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
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.*

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Billing and invoice management")
@SecurityRequirement(name = "Bearer")
class BillingController(
    private val billingService: BillingService,
    private val pdfGeneratorService: PdfGeneratorService,
    private val localeResolver: LocaleResolver
) {

    @GetMapping
    @Operation(summary = "Get extended details for a specific service", description = "Gets the current billing balance and account number for the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Detailed service information including hardware and TV packages", content = [Content(schema = Schema(implementation = BillingSummaryResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getBillingSummary(): ResponseEntity<BillingSummaryResponse> {
        val summary = billingService.getBillingSummary(authenticatedClientNumber())
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/invoices")
    @Operation(summary = "Get all invoices", description = "Returns all invoices for the currently authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns list of invoices", content = [Content(array = ArraySchema(schema = Schema(implementation = InvoiceResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getInvoices(): ResponseEntity<List<InvoiceResponse>> {
        val invoices = billingService.getClientInvoices(authenticatedClientNumber())
        return ResponseEntity.ok(invoices)
    }

    @GetMapping("/invoices/{invoice_id}")
    @Operation(summary = "Get extended details for a specific invoice", description = "Gets detailed breakdown of a specific invoice including dates, seller, buyer and line items.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(schema = Schema(implementation = InvoiceDetailResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Invoice not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getInvoiceDetails(
        @Parameter(description = "ID of the invoice", required = true)
        @PathVariable("invoice_id") invoiceId: Long
    ): ResponseEntity<InvoiceDetailResponse> {
        val invoiceDetails = billingService.getInvoiceDetails(invoiceId, authenticatedClientNumber())
        return ResponseEntity.ok(invoiceDetails)
    }

    @GetMapping("/invoices/{invoice_id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(summary = "Download invoice as PDF", description = "Generates and returns a PDF version of the requested invoice.")
    @ApiResponse(responseCode = "200", description = "The invoice file in PDF format", content = [Content(mediaType = "application/pdf", schema = Schema(type = "string", format = "binary"))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Invoice not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun downloadInvoicePdf(
        @Parameter(description = "ID of the invoice", required = true)
        @PathVariable("invoice_id") invoiceId: Long,
        request: HttpServletRequest
    ): ResponseEntity<ByteArrayResource> {
        val locale = localeResolver.resolveLocale(request)
        val invoiceDetails = billingService.getInvoiceDetails(invoiceId, authenticatedClientNumber())
        val pdfBytes = pdfGeneratorService.generateInvoicePdf(invoiceDetails, locale)
        val resource = ByteArrayResource(pdfBytes)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_$invoiceId.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfBytes.size.toLong())
            .body(resource)
    }

    @PostMapping("/payments/{invoice_id}")
    @Operation(summary = "Process a payment for a specific invoice for the authenticated user", description = "Processes a payment for a specific invoice for the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Payment for the invoice added successfully", content = [Content(schema = Schema(implementation = PaymentResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error or invalid payment request", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Invoice not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun processPayment(
        @Parameter(description = "ID of the invoice", required = true)
        @PathVariable("invoice_id") invoiceId: Long,
        @Valid @RequestBody request: PaymentRequest
    ): ResponseEntity<PaymentResponse> {
        val response = billingService.processPayment(authenticatedClientNumber(), invoiceId, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/payments")
    @Operation(summary = "Get a payments list for the authenticated user", description = "Returns a list of all successful and pending payments for the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(array = ArraySchema(schema = Schema(implementation = PaymentHistoryResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getPaymentHistory(): ResponseEntity<List<PaymentHistoryResponse>> {
        val payments = billingService.getPaymentHistory(authenticatedClientNumber())
        return ResponseEntity.ok(payments)
    }

    @GetMapping("/returns")
    @Operation(summary = "Get a returns list for the authenticated user", description = "Returns a list of processed, accepted, and rejected returns for the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(array = ArraySchema(schema = Schema(implementation = ReturnResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getReturns(): ResponseEntity<List<ReturnResponse>> {
        val returns = billingService.getReturns(authenticatedClientNumber())
        return ResponseEntity.ok(returns)
    }

    @PostMapping("/returns")
    @Operation(summary = "Create a request for a return for the authenticated user", description = "Initiates a return money request for the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Bad request - One or more parameters are invalid", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun createReturnRequest(@Valid @RequestBody request: CreateReturnRequest): ResponseEntity<Void> {
        billingService.processReturnRequest(authenticatedClientNumber(), request)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/transfer-details")
    @Operation(summary = "Get transfer details for the authenticated user", description = "Returns the receiver, address and bank account number to use for bank transfers.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(schema = Schema(implementation = TransferDetailsResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Transfer details not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getTransferDetails(): ResponseEntity<TransferDetailsResponse> {
        val details = billingService.getTransferDetails(authenticatedClientNumber())
        return ResponseEntity.ok(details)
    }

    private fun authenticatedClientNumber(): String =
        org.springframework.security.core.context.SecurityContextHolder.getContext().authentication!!.name
}