package pl.elcaro.elcarobokapp.domain.payments

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "refunds")
class Refund(
    @Column(nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal,

    @Column(name = "refund_date", nullable = false)
    var refundDate: Instant = Instant.now(),

    @Column(nullable = false, length = 20)
    var status: String = "COMPLETED",

    @Column(length=255)
    var reason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    var payment: Payment,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}