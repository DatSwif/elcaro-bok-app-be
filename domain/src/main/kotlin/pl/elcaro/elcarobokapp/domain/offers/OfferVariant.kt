package pl.elcaro.elcarobokapp.domain.offers

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "offer_variants")
class OfferVariant(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    var offer: Offer,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "regular_price", nullable = false, precision = 10, scale = 2)
    var regularPrice: BigDecimal,

    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    var currentPrice: BigDecimal,

    @Column(nullable = false, length = 20)
    var period: String = "MONTH",

    @Column(name = "promo_description", nullable = false, length = 1000)
    var promoDescription: String = "",

    @Column(name = "blocks_json", nullable = false, columnDefinition = "TEXT")
    var blocksJson: String = "[]",
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
