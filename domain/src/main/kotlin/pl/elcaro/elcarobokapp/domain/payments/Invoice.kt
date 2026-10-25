package pl.elcaro.elcarobokapp.domain.payments

import jakarta.persistence.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "invoices")
class Invoice (
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    var invoiceNumber: String,

    @Column(name = "issue_date", nullable = false)
    var issueDate: Instant,

    @Column(name = "due_date", nullable = false)
    var dueDate: Instant,

    @Column(name = "period_start", nullable = false)
    var periodStart: Instant,

    @Column(name = "period_end")
    var periodEnd: Instant? = null,

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InvoiceStatus = InvoiceStatus.UNPAID,

    @Column(name = "document_id")
    var documentId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}