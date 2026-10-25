package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class HardwareTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "hardware_id")
    var hardwareId: Long = 0,
) : Serializable

@Entity
@Table(name = "hardware_translations")
class HardwareTranslation(
    @EmbeddedId
    var id: HardwareTranslationId = HardwareTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("hardwareId")
    @JoinColumn(name = "hardware_id")
    var hardware: Hardware? = null,

    @Column(nullable = false, length = 100)
    var name: String = "",
)
