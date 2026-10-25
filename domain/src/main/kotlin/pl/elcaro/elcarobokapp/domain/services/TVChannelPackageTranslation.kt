package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class TVChannelPackageTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "tv_channel_package_id")
    var tvChannelPackageId: Long = 0,
) : Serializable

@Entity
@Table(name = "tv_channel_package_translations")
class TVChannelPackageTranslation(
    @EmbeddedId
    var id: TVChannelPackageTranslationId = TVChannelPackageTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tvChannelPackageId")
    @JoinColumn(name = "tv_channel_package_id")
    var tvChannelPackage: TVChannelPackage? = null,

    @Column(nullable = false)
    var name: String = "",
)
