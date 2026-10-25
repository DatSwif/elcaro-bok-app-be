package pl.elcaro.elcarobokapp.domain.offers

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class OfferTranslationId(
    @Column(name = "language_code", length = 10)
    var languageCode: String = "",

    @Column(name = "offer_id")
    var offerId: Long = 0,
) : Serializable

@Entity
@Table(name = "offer_translations")
class OfferTranslation(
    @EmbeddedId
    var id: OfferTranslationId = OfferTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("offerId")
    @JoinColumn(name = "offer_id")
    var offer: Offer? = null,

    @Column(nullable = false, length = 255)
    var title: String = "",

    @Column(nullable = false, length = 500)
    var subtitle: String = "",
)
