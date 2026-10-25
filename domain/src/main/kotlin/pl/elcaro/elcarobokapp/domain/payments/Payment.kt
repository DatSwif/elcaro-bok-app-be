package pl.elcaro.elcarobokapp.domain.payments

import jakarta.persistence.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "payments")
class Payment(
    @Column(nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal,

    @Column(name = "payment_date", nullable = false)
    var paymentDate: Instant = Instant.now(),

    @Column(name = "payment_method", nullable = false, length = 50)
    var paymentMethod: String, // bank_transfer, card, blik

    @Column(name = "transaction_id", length = 100)
    var transactionId: String? = null,

    @Column(length = 255)
    var title: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    var invoice: Invoice? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}