package pl.elcaro.elcarobokapp.domain.offers

import jakarta.persistence.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.time.Instant

@Entity
@Table(name = "offer_orders")
class OfferOrder(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_variant_id", nullable = false)
    var variant: OfferVariant,

    @Column(name = "ordered_at", nullable = false)
    var orderedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
