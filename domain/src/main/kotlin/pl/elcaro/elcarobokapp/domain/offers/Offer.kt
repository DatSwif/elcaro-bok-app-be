package pl.elcaro.elcarobokapp.domain.offers

import jakarta.persistence.*

@Entity
@Table(name = "offers")
class Offer(
    @Column(name = "image_url")
    var imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: OfferCategory,

    @Column(nullable = false, length = 500)
    var tags: String = "",

    @Column(nullable = false)
    var featured: Boolean = false,

    @Column(name = "legal_note", nullable = false, columnDefinition = "TEXT")
    var legalNote: String = "",
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "offer", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableSet<OfferTranslation> = mutableSetOf()

    @OneToMany(mappedBy = "offer", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    var variants: MutableList<OfferVariant> = mutableListOf()
}

enum class OfferCategory { INTERNET, TV, INTERNET_TV, MOBILE, DEVICES }
