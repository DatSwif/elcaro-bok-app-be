package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class BillingSummaryResponse(
    val balance: BigDecimal,
    @get:JsonProperty("account_number") val accountNumber: String
)

data class InvoiceDetailResponse(
    val id: Long,
    @get:JsonProperty("invoice_number") val invoiceNumber: String,
    @get:JsonProperty("document_id") val documentId: String?,
    val status: String,
    val dates: InvoiceDates,
    val seller: Seller,
    val buyer: Buyer,
    @get:JsonProperty("line_items") val lineItems: List<LineItem>,
    val totals: Totals,
    @get:JsonProperty("payment_details") val paymentDetails: PaymentDetails
)

data class InvoiceDates(
    @get:JsonProperty("issue_date") val issueDate: LocalDate,
    @get:JsonProperty("sale_date") val saleDate: LocalDate,
    @get:JsonProperty("due_date") val dueDate: LocalDate,
    @get:JsonProperty("period_start") val periodStart: LocalDate,
    @get:JsonProperty("period_end") val periodEnd: LocalDate
)

data class Seller(
    val name: String,
    val address: String,
    val nip: String
)

data class Buyer(
    val name: String,
    @get:JsonProperty("subscriber_number") val subscriberNumber: String,
    val address: String
)

data class LineItem(
    val description: String,
    val quantity: Int,
    val unit: String,
    @get:JsonProperty("vat_rate") val vatRate: Int,
    @get:JsonProperty("net_price") val netPrice: BigDecimal,
    @get:JsonProperty("gross_price") val grossPrice: BigDecimal
)

data class Totals(
    @get:JsonProperty("total_net") val totalNet: BigDecimal,
    @get:JsonProperty("total_gross") val totalGross: BigDecimal
)

data class PaymentDetails(
    val method: String,
    @get:JsonProperty("bank_account") val bankAccount: String
)

data class InvoiceResponse(
    val id: Long,
    @get:JsonProperty("invoice_number") val invoiceNumber: String,
    @get:JsonProperty("issue_date") val issueDate: Instant,
    @get:JsonProperty("due_date") val dueDate: Instant,
    @get:JsonProperty("period_start") val periodStart: Instant,
    @get:JsonProperty("period_end") val periodEnd: Instant?,
    @get:JsonProperty("total_amount") val totalAmount: BigDecimal,
    val status: String,
    @get:JsonProperty("document_id") val documentId: String?
)

data class PaymentRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    var amount: BigDecimal,

    @field:NotBlank(message = "Payment method is required")
    @field:Size(max = 50, message = "Payment method must not exceed 50 characters")
    @get:JsonProperty("payment_method") val paymentMethod: String
)

data class PaymentResponse(
    val paymentId: Long,
    val status: String,
    val transactionId: String
)

data class PaymentHistoryResponse(
    val id: Long,
    @get:JsonProperty("issue_date") val issueDate: String,
    @get:JsonProperty("start_date") val startDate: String,
    @get:JsonProperty("end_date") val endDate: String,
    @get:JsonProperty("payment_title") val paymentTitle: String,
    @get:JsonProperty("payment_date") val paymentDate: String,
    val status: String,
    val amount: BigDecimal
)

data class ReturnResponse(
    val id: Long,
    @get:JsonProperty("account_number") val accountNumber: String,
    @get:JsonProperty("issue_date") val issueDate: String,
    val status: String,
    val amount: BigDecimal
)

data class CreateReturnRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 50, message = "First name must not exceed 50 characters")
    @get:JsonProperty("first_name") val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 50, message = "Last name must not exceed 50 characters")
    @get:JsonProperty("last_name") val lastName: String,

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    var amount: BigDecimal,

    @field:NotBlank(message = "Account number is required")
    @field:Size(max = 34, message = "Account number must not exceed 34 characters")
    @get:JsonProperty("account_number") val accountNumber: String
)

data class TransferDetailsResponse(
    val receiver: String,
    @get:JsonProperty("receiver_address") val receiverAddress: String,
    @get:JsonProperty("bank_account") val bankAccount: String
)
