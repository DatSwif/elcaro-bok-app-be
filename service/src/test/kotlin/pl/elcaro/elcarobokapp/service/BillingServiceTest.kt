package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import pl.elcaro.elcarobokapp.domain.payments.Invoice
import pl.elcaro.elcarobokapp.domain.payments.InvoiceStatus
import pl.elcaro.elcarobokapp.domain.payments.Payment
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.InvoiceRepository
import pl.elcaro.elcarobokapp.infrastructure.PaymentRepository
import java.math.BigDecimal
import java.time.Instant

class BillingServiceTest : DescribeSpec({

    val invoiceRepository = mockk<InvoiceRepository>()
    val paymentRepository = mockk<PaymentRepository>()
    val clientRepository = mockk<ClientRepository>()
    val service = BillingService(invoiceRepository, paymentRepository, clientRepository)

    beforeEach { clearAllMocks() }

    fun client(
        id: Long = 1L,
        firstName: String? = "Jan",
        lastName: String? = "Kowalski",
    ) = Client(
        clientNumber = "C001",
        firstName = firstName,
        lastName = lastName,
        languageCode = "pl",
    ).also { it.id = id }

    fun invoice(
        id: Long = 1L,
        clientRef: Client = client(),
        total: BigDecimal = BigDecimal("100.00"),
        status: InvoiceStatus = InvoiceStatus.UNPAID,
        periodStart: Instant = Instant.parse("2024-01-01T00:00:00Z"),
        periodEnd: Instant? = Instant.parse("2024-01-31T00:00:00Z"),
    ) = Invoice(
        invoiceNumber = "F/$id",
        issueDate = Instant.parse("2024-01-15T00:00:00Z"),
        dueDate = Instant.parse("2024-01-31T00:00:00Z"),
        periodStart = periodStart,
        periodEnd = periodEnd,
        totalAmount = total,
        status = status,
        client = clientRef,
    ).also { it.id = id }

    fun payment(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("50.00"),
        invoice: Invoice? = null,
        clientRef: Client = client(),
    ) = Payment(
        amount = amount,
        paymentMethod = "bank_transfer",
        client = clientRef,
        invoice = invoice,
        title = invoice?.let { "Payment for ${it.invoiceNumber}" },
    ).also { it.id = id }

    describe("getBillingSummary") {
        it("throws when client not found") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { service.getBillingSummary("X") }
        }

        it("computes negative balance as standalonePayments - unpaidTotal") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { invoiceRepository.findAllByClientClientNumber("C001") } returns listOf(
                invoice(1, total = BigDecimal("100.00")),
                invoice(2, total = BigDecimal("50.00")),
                invoice(3, total = BigDecimal("999.00"), status = InvoiceStatus.PAID), // filtered out
            )
            every { paymentRepository.findAllByClientClientNumber("C001") } returns listOf(
                payment(amount = BigDecimal("20.00"), invoice = null),
                payment(amount = BigDecimal("999.00"), invoice = invoice(99)), // not standalone
            )

            val summary = service.getBillingSummary("C001")

            summary.balance shouldBe BigDecimal("-130.00") // 20 - 150
            summary.accountNumber shouldContain "00000001"
        }

        it("returns zero balance when no invoices and no payments") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { invoiceRepository.findAllByClientClientNumber("C001") } returns emptyList()
            every { paymentRepository.findAllByClientClientNumber("C001") } returns emptyList()

            service.getBillingSummary("C001").balance shouldBe BigDecimal.ZERO
        }
    }

    describe("getClientInvoices") {
        it("returns empty list when client has no invoices") {
            every { invoiceRepository.findAllByClientClientNumber("C001") } returns emptyList()
            service.getClientInvoices("C001").shouldBeEmpty()
        }

        it("maps invoices including status name and documentId") {
            val inv = invoice(7).apply { documentId = "doc-7" }
            every { invoiceRepository.findAllByClientClientNumber("C001") } returns listOf(inv)

            val list = service.getClientInvoices("C001")

            list shouldHaveSize 1
            list[0].invoiceNumber shouldBe "F/7"
            list[0].status shouldBe "UNPAID"
            list[0].documentId shouldBe "doc-7"
        }
    }

    describe("getInvoiceDetails") {
        it("throws when invoice not found") {
            every { invoiceRepository.findByIdAndClientClientNumber(99, "C001") } returns null

            shouldThrow<NoSuchElementException> { service.getInvoiceDetails(99, "C001") }
        }

        it("uses periodStart + 1 month when periodEnd is null") {
            val inv = invoice(periodEnd = null)
            every { invoiceRepository.findByIdAndClientClientNumber(1, "C001") } returns inv
            every { clientRepository.findByClientNumber("C001") } returns client()

            val details = service.getInvoiceDetails(1, "C001")

            // periodStart is 2024-01-01, plus 1 month = 2024-02-01 (system default zone)
            details.dates.periodEnd.year shouldBe 2024
            details.dates.periodEnd.monthValue shouldBe 2
        }

        it("falls back to 'Client' buyer name when both firstName and lastName are null") {
            val inv = invoice()
            every { invoiceRepository.findByIdAndClientClientNumber(1, "C001") } returns inv
            every { clientRepository.findByClientNumber("C001") } returns client(firstName = null, lastName = null)

            service.getInvoiceDetails(1, "C001").buyer.name shouldBe "Client"
        }

        it("uses concatenated firstName + lastName as buyer name when present") {
            val inv = invoice()
            every { invoiceRepository.findByIdAndClientClientNumber(1, "C001") } returns inv
            every { clientRepository.findByClientNumber("C001") } returns client()

            service.getInvoiceDetails(1, "C001").buyer.name shouldBe "Jan Kowalski"
        }
    }

    describe("processPayment") {
        val request = PaymentRequest(amount = BigDecimal("100.00"), paymentMethod = "card")

        it("throws when client not found") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { service.processPayment("X", 1, request) }
        }

        it("throws when invoice not found") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { invoiceRepository.findByIdAndClientClientNumber(99, "C001") } returns null

            shouldThrow<NoSuchElementException> { service.processPayment("C001", 99, request) }
        }

        it("throws IllegalStateException when invoice is already paid") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { invoiceRepository.findByIdAndClientClientNumber(1, "C001") } returns
                invoice(status = InvoiceStatus.PAID)

            shouldThrow<IllegalStateException> { service.processPayment("C001", 1, request) }
        }

        it("saves payment, marks invoice PAID, and returns a completed response") {
            val inv = invoice()
            val c = client()
            every { clientRepository.findByClientNumber("C001") } returns c
            every { invoiceRepository.findByIdAndClientClientNumber(1, "C001") } returns inv
            val savedPayment = slot<Payment>()
            every { paymentRepository.save(capture(savedPayment)) } answers {
                savedPayment.captured.also { it.id = 99 }
            }
            every { invoiceRepository.save(inv) } returns inv

            val response = service.processPayment("C001", 1, request)

            response.paymentId shouldBe 99
            response.status shouldBe "COMPLETED"
            inv.status shouldBe InvoiceStatus.PAID
            verify(exactly = 1) { invoiceRepository.save(inv) }
        }
    }

    describe("getPaymentHistory") {
        it("throws when client not found") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { service.getPaymentHistory("X") }
        }

        it("uses payment.paymentDate when invoice is null") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { paymentRepository.findAllByClientClientNumber("C001") } returns listOf(
                payment(invoice = null),
            )

            val list = service.getPaymentHistory("C001")

            list shouldHaveSize 1
            list[0].paymentTitle shouldBe "Opłata"
        }

        it("uses invoice fields when invoice is present") {
            val inv = invoice(7)
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { paymentRepository.findAllByClientClientNumber("C001") } returns listOf(
                payment(invoice = inv),
            )

            val list = service.getPaymentHistory("C001")

            list[0].paymentTitle shouldBe "Payment for F/7"
        }
    }

    describe("getTransferDetails") {
        it("throws when client not found") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { service.getTransferDetails("X") }
        }

        it("returns static transfer details for an existing client") {
            every { clientRepository.findByClientNumber("C001") } returns client()

            val details = service.getTransferDetails("C001")

            details.receiver shouldBe "Elcaro Sp. z o.o."
            details.bankAccount shouldBe "PL 82 1020 5226 0000 6102 0417 7895"
            details.receiverAddress shouldBe "Willowa 10B, 20-819 Lublin"
        }
    }

    describe("getReturns") {
        it("throws when client not found") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { service.getReturns("X") }
        }

        it("returns mocked list of returns when client exists") {
            // Note: this method currently returns hardcoded mock data — replace when DB-backed.
            every { clientRepository.findByClientNumber("C001") } returns client()

            service.getReturns("C001") shouldHaveSize 3
        }
    }

    describe("processReturnRequest") {
        val baseReq = CreateReturnRequest(
            firstName = "Jan",
            lastName = "Kowalski",
            amount = BigDecimal("100.00"),
            accountNumber = "PL00",
        )

        it("throws when client not found") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { service.processReturnRequest("X", baseReq) }
        }

        it("throws IllegalArgumentException when amount is zero or negative") {
            every { clientRepository.findByClientNumber("C001") } returns client()

            shouldThrow<IllegalArgumentException> {
                service.processReturnRequest("C001", baseReq.copy(amount = BigDecimal.ZERO))
            }
            shouldThrow<IllegalArgumentException> {
                service.processReturnRequest("C001", baseReq.copy(amount = BigDecimal("-1")))
            }
        }

        it("succeeds for a valid request") {
            every { clientRepository.findByClientNumber("C001") } returns client()

            service.processReturnRequest("C001", baseReq) // no exception
        }
    }
})
