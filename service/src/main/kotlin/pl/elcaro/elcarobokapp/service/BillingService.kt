package pl.elcaro.elcarobokapp.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.payments.InvoiceStatus
import pl.elcaro.elcarobokapp.domain.payments.Payment
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.InvoiceRepository
import pl.elcaro.elcarobokapp.infrastructure.PaymentRepository
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@Transactional
class BillingService(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val clientRepository: ClientRepository
) {
    private val log = LoggerFactory.getLogger(BillingService::class.java)

    @Transactional(readOnly = true)
    fun getBillingSummary(clientNumber: String): BillingSummaryResponse {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        val unpaidInvoices = invoiceRepository.findAllByClientClientNumber(clientNumber)
            .filter { it.status == InvoiceStatus.UNPAID }

        val unpaidTotal = unpaidInvoices.fold(BigDecimal.ZERO) { acc, invoice -> acc.add(invoice.totalAmount) }
        val standalonePayments = paymentRepository.findAllByClientClientNumber(clientNumber)
            .filter { it.invoice == null }
            .fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amount) }

        val balance = standalonePayments.subtract(unpaidTotal)
        
        // Mock account number based on client ID
        val accountSuffix = client.id.toString().padStart(8, '0')
        val mockAccountNumber = "PL 82 1020 5226 0000 6102 $accountSuffix"

        return BillingSummaryResponse(
            balance = balance,
            accountNumber = mockAccountNumber
        )
    }

    @Transactional(readOnly = true)
    fun getClientInvoices(clientNumber: String): List<InvoiceResponse> {
        return invoiceRepository.findAllByClientClientNumber(clientNumber)
            .map { invoice ->
                InvoiceResponse(
                    id = invoice.id!!,
                    invoiceNumber = invoice.invoiceNumber,
                    issueDate = invoice.issueDate,
                    dueDate = invoice.dueDate,
                    periodStart = invoice.periodStart,
                    periodEnd = invoice.periodEnd,
                    totalAmount = invoice.totalAmount,
                    status = invoice.status.name,
                    documentId = invoice.documentId
                )
            }
    }

    @Transactional(readOnly = true)
    fun getInvoiceDetails(invoiceId: Long, clientNumber: String): InvoiceDetailResponse {
        val invoice = invoiceRepository.findByIdAndClientClientNumber(invoiceId, clientNumber)
            ?: throw NoSuchElementException("Invoice with ID $invoiceId not found or access denied")
            
        val client = clientRepository.findByClientNumber(clientNumber)!!

        val zoneId = ZoneId.systemDefault()
        
        // Construct mock response since detailed line items aren't stored in DB
        return InvoiceDetailResponse(
            id = invoice.id!!,
            invoiceNumber = invoice.invoiceNumber,
            documentId = invoice.documentId,
            status = invoice.status.name,
            dates = InvoiceDates(
                issueDate = invoice.issueDate.atZone(zoneId).toLocalDate(),
                saleDate = invoice.issueDate.atZone(zoneId).toLocalDate(),
                dueDate = invoice.dueDate.atZone(zoneId).toLocalDate(),
                periodStart = invoice.periodStart.atZone(zoneId).toLocalDate(),
                periodEnd = invoice.periodEnd?.atZone(zoneId)?.toLocalDate() ?: invoice.periodStart.atZone(zoneId).toLocalDate().plusMonths(1)
            ),
            seller = Seller(
                name = "Elcaro Sp. z o.o.",
                address = "ul. Testowa 1, 00-001 Warszawa",
                nip = "1234567890"
            ),
            buyer = Buyer(
                name = "${client.firstName ?: ""} ${client.lastName ?: ""}".trim().takeIf { it.isNotEmpty() } ?: "Client",
                subscriberNumber = clientNumber,
                address = "Adres Klienta, 00-000 Miasto" // Mock since client_addresses aren't directly linked here simply
            ),
            lineItems = listOf(
                LineItem(
                    description = "Abonament za usługi telekomunikacyjne",
                    quantity = 1,
                    unit = "szt.",
                    vatRate = 23,
                    netPrice = invoice.totalAmount.divide(BigDecimal("1.23"), 2, java.math.RoundingMode.HALF_UP),
                    grossPrice = invoice.totalAmount
                )
            ),
            totals = Totals(
                totalNet = invoice.totalAmount.divide(BigDecimal("1.23"), 2, java.math.RoundingMode.HALF_UP),
                totalGross = invoice.totalAmount
            ),
            paymentDetails = PaymentDetails(
                method = "przelew",
                bankAccount = "PL 82 1020 5226 0000 6102 0417 7895"
            )
        )
    }

    fun processPayment(clientNumber: String, invoiceId: Long, request: PaymentRequest): PaymentResponse {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        val invoice = invoiceRepository.findByIdAndClientClientNumber(invoiceId, clientNumber)
            ?: throw NoSuchElementException("Invoice not found or access denied")

        if (invoice.status == InvoiceStatus.PAID) {
            throw IllegalStateException("Invoice is already paid")
        }

        val transactionId = UUID.randomUUID().toString()

        val payment = Payment(
            amount = request.amount,
            paymentMethod = request.paymentMethod,
            transactionId = transactionId,
            title = "Payment for invoice ${invoice.invoiceNumber}",
            client = client,
            invoice = invoice
        )

        val savedPayment = paymentRepository.save(payment)

        invoice.status = InvoiceStatus.PAID
        invoiceRepository.save(invoice)

        return PaymentResponse(
            paymentId = savedPayment.id!!,
            status = "COMPLETED",
            transactionId = transactionId
        )
    }

    @Transactional(readOnly = true)
    fun getPaymentHistory(clientNumber: String): List<PaymentHistoryResponse> {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

        return paymentRepository.findAllByClientClientNumber(clientNumber)
            .map { payment ->
                // Derive dates from invoice if available, else use payment creation date
                val invoice = payment.invoice
                val issueDate = invoice?.issueDate ?: payment.paymentDate
                val startDate = invoice?.periodStart ?: payment.paymentDate
                val endDate = invoice?.periodEnd ?: payment.paymentDate
                val paymentDate = payment.paymentDate

                PaymentHistoryResponse(
                    id = payment.id!!,
                    issueDate = formatter.format(issueDate),
                    startDate = formatter.format(startDate),
                    endDate = formatter.format(endDate),
                    paymentTitle = payment.title ?: "Opłata",
                    paymentDate = formatter.format(paymentDate),
                    status = "Paid", // Simplified for now
                    amount = payment.amount
                )
            }
    }

    @Transactional(readOnly = true)
    fun getReturns(clientNumber: String): List<ReturnResponse> {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        // Mock response
        val accountSuffix = client.id.toString().padStart(8, '0')
        val mockAccountNumber = "PL 821020522600006102$accountSuffix"
        
        return listOf(
            ReturnResponse(
                id = 1L,
                accountNumber = mockAccountNumber,
                issueDate = "12.02.2025",
                status = "Pending",
                amount = BigDecimal("500.00")
            ),
            ReturnResponse(
                id = 2L,
                accountNumber = mockAccountNumber,
                issueDate = "30.01.2025",
                status = "Accepted",
                amount = BigDecimal("169.99")
            ),
            ReturnResponse(
                id = 3L,
                accountNumber = mockAccountNumber,
                issueDate = "30.01.2025",
                status = "Rejected",
                amount = BigDecimal("169.99")
            )
        )
    }

    @Transactional(readOnly = true)
    fun getTransferDetails(clientNumber: String): TransferDetailsResponse {
        clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        return TransferDetailsResponse(
            receiver = "Elcaro Sp. z o.o.",
            receiverAddress = "Willowa 10B, 20-819 Lublin",
            bankAccount = "PL 82 1020 5226 0000 6102 0417 7895"
        )
    }

    fun processReturnRequest(clientNumber: String, request: CreateReturnRequest) {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        // Basic validation
        if (request.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Return amount must be greater than zero")
        }
        
        // Mock processing the return request
        log.info("Processing return request for client $clientNumber. Amount: ${request.amount}, Account: ${request.accountNumber}")
    }
}
