package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class ConsentTypeTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "consent_type_id")
    var consentTypeId: Int = 0,
) : Serializable

@Entity
@Table(name = "consent_type_translations")
class ConsentTypeTranslation(
    @EmbeddedId
    var id: ConsentTypeTranslationId = ConsentTypeTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("consentTypeId")
    @JoinColumn(name = "consent_type_id")
    var consentType: ConsentType? = null,

    @Column(length = 50)
    var code: String? = null,

    @Column(length = 255)
    var title: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,
)
