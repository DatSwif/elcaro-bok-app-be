package pl.elcaro.elcarobokapp.domain.messages

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class GlobalMessageTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "global_message_id")
    var globalMessageId: Long = 0,
) : Serializable

@Entity
@Table(name = "global_message_translations")
class GlobalMessageTranslation(
    @EmbeddedId
    var id: GlobalMessageTranslationId = GlobalMessageTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("globalMessageId")
    @JoinColumn(name = "global_message_id")
    var globalMessage: GlobalMessage? = null,

    @Column(nullable = false, length = 255)
    var title: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = "",
)
