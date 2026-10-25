package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class TVChannelTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "tv_channel_id")
    var tvChannelId: Long = 0,
) : Serializable

@Entity
@Table(name = "tv_channel_translations")
class TVChannelTranslation(
    @EmbeddedId
    var id: TVChannelTranslationId = TVChannelTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tvChannelId")
    @JoinColumn(name = "tv_channel_id")
    var tvChannel: TVChannel? = null,

    @Column(nullable = false, length = 30)
    var name: String = "",
)
