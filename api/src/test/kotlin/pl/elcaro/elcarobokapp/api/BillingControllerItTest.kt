package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pl.elcaro.elcarobokapp.domain.payments.Invoice
import pl.elcaro.elcarobokapp.domain.payments.InvoiceStatus
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.InvoiceRepository
import pl.elcaro.elcarobokapp.infrastructure.PaymentRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import pl.elcaro.elcarobokapp.service.PaymentRequest
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

class BillingControllerItTest : BaseIntegrationTest({

    val self = this as BillingControllerItTest

    beforeTest {
        self.paymentRepository.deleteAll()
        self.invoiceRepository.deleteAll()
        self.userConsentRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    afterTest {
        self.paymentRepository.deleteAll()
        self.invoiceRepository.deleteAll()
    }

    fun savedClientWithToken(clientNumber: String = "C001"): Pair<Client, String> {
        val client = self.clientRepository.save(Client(clientNumber = clientNumber, languageCode = "pl"))
        val token = self.jwtTokenProvider.generateToken(clientNumber)
        return client to token
    }

    fun savedInvoice(client: Client, amount: String = "150.00", status: InvoiceStatus = InvoiceStatus.UNPAID): Invoice {
        return self.invoiceRepository.save(
            Invoice(
                invoiceNumber = "INV-${Instant.now().toEpochMilli()}",
                issueDate = Instant.now().minus(5, ChronoUnit.DAYS),
                dueDate = Instant.now().plus(9, ChronoUnit.DAYS),
                periodStart = Instant.now().minus(35, ChronoUnit.DAYS),
                totalAmount = BigDecimal(amount),
                status = status,
                client = client
            )
        )
    }

    describe("GET /api/billing") {
        it("returns 200 OK and billing summary") {
            val (client, token) = savedClientWithToken()
            savedInvoice(client, "100.50", InvoiceStatus.UNPAID)
            savedInvoice(client, "50.00", InvoiceStatus.UNPAID)

            self.mockMvc.get("/api/billing") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.balance") { value(-150.50) }
                jsonPath("$.account_number") { exists() }
            }
        }
    }

    describe("GET /api/billing/invoices") {
        it("returns 200 and a list of invoices for the authenticated user") {
            val (client, token) = savedClientWithToken()
            savedInvoice(client, "100.50")
            savedInvoice(client, "50.00", InvoiceStatus.PAID)

            self.mockMvc.get("/api/billing/invoices") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].total_amount") { value(100.5) }
                jsonPath("$[0].status") { value("UNPAID") }
            }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/billing/invoices") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("GET /api/billing/invoices/{invoice_id}") {
        it("returns 200 OK and invoice details") {
            val (client, token) = savedClientWithToken()
            val invoice = savedInvoice(client, "123.00")

            self.mockMvc.get("/api/billing/invoices/${invoice.id}") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(invoice.id) }
                jsonPath("$.invoice_number") { value(invoice.invoiceNumber) }
                jsonPath("$.totals.total_gross") { value(123.00) }
                jsonPath("$.line_items") { isArray() }
            }
        }

        it("returns 404 when invoice not found") {
            val (_, token) = savedClientWithToken()
            self.mockMvc.get("/api/billing/invoices/99999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("POST /api/billing/payments/{invoice_id}") {
        it("returns 200, creates payment and marks invoice as PAID") {
            val (client, token) = savedClientWithToken()
            val invoice = savedInvoice(client, "150.00")
            val request = PaymentRequest(amount = BigDecimal("150.00"), paymentMethod = "BLIK")

            self.mockMvc.post("/api/billing/payments/${invoice.id}") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("COMPLETED") }
                jsonPath("$.transactionId") { exists() }
            }

            // Verify database state
            val updatedInvoice = self.invoiceRepository.findById(invoice.id!!).get()
            assert(updatedInvoice.status == InvoiceStatus.PAID)

            val payments = self.paymentRepository.findAll().toList()
            assert(payments.size == 1)
            assert(payments.first().amount.compareTo(BigDecimal("150.00")) == 0)
        }

        it("returns error (or 404/500 depending on GlobalExceptionHandler) when trying to pay someone else's invoice") {
            val (_, hackerToken) = savedClientWithToken("HACKER")
            val (victim, _) = savedClientWithToken("VICTIM")
            val victimInvoice = savedInvoice(victim, "1000.00")

            val request = PaymentRequest(amount = BigDecimal("1000.00"), paymentMethod = "CARD")

            self.mockMvc.post("/api/billing/payments/${victimInvoice.id}") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $hackerToken")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }
    describe("GET /api/billing/payments") {
        it("returns 200 and a list of payments") {
            val (client, token) = savedClientWithToken()

            self.paymentRepository.save(
                pl.elcaro.elcarobokapp.domain.payments.Payment(
                    amount = BigDecimal("99.99"),
                    paymentMethod = "BLIK",
                    transactionId = "TXN-123",
                    title = "Test Payment",
                    client = client,
                    invoice = null
                )
            )

            self.mockMvc.get("/api/billing/payments") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].amount") { value(99.99) }
                jsonPath("$[0].payment_title") { value("Test Payment") }
            }
        }
    }

    describe("GET /api/billing/returns") {
        it("returns 200 and a mocked list of returns") {
            val (_, token) = savedClientWithToken()

            self.mockMvc.get("/api/billing/returns") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                // Сервис пока возвращает 3 захардкоженных возврата
                jsonPath("$.length()") { value(3) }
                jsonPath("$[0].status") { value("Pending") }
                jsonPath("$[0].account_number") { exists() }
            }
        }
    }

    describe("GET /api/billing/transfer-details") {
        it("returns 200 OK and transfer details for the authenticated user") {
            val (_, token) = savedClientWithToken()

            self.mockMvc.get("/api/billing/transfer-details") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.receiver") { value("Elcaro Sp. z o.o.") }
                jsonPath("$.receiver_address") { value("Willowa 10B, 20-819 Lublin") }
                jsonPath("$.bank_account") { value("PL 82 1020 5226 0000 6102 0417 7895") }
            }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/billing/transfer-details") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 404 when client does not exist") {
            val unknownToken = self.jwtTokenProvider.generateToken("GHOST-CLIENT")

            self.mockMvc.get("/api/billing/transfer-details") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $unknownToken")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("POST /api/billing/returns") {
        it("returns 200 on successful return request") {
            val (_, token) = savedClientWithToken()
            val request = pl.elcaro.elcarobokapp.service.CreateReturnRequest(
                firstName = "Jan",
                lastName = "Kowalski",
                amount = BigDecimal("50.00"),
                accountNumber = "PL1234567890"
            )

            self.mockMvc.post("/api/billing/returns") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
            }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var invoiceRepository: InvoiceRepository
    @Autowired lateinit var paymentRepository: PaymentRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
    @Autowired lateinit var userConsentRepository: UserConsentRepository
}
