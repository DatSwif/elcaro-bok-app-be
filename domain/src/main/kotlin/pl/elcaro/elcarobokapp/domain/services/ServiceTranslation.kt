package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class ServiceTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "service_id")
    var serviceId: Long = 0,
) : Serializable

@Entity
@Table(name = "service_translations")
class ServiceTranslation(
    @EmbeddedId
    var id: ServiceTranslationId = ServiceTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    var service: Service? = null,

    @Column(length = 30)
    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var info: String? = null,
)
