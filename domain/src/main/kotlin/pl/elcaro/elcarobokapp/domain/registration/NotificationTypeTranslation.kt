package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class NotificationTypeTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "notification_type_id")
    var notificationTypeId: Int = 0,
) : Serializable

@Entity
@Table(name = "notification_type_translations")
class NotificationTypeTranslation(
    @EmbeddedId
    var id: NotificationTypeTranslationId = NotificationTypeTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("notificationTypeId")
    @JoinColumn(name = "notification_type_id")
    var notificationType: NotificationType? = null,

    @Column(length = 100)
    var name: String? = null,
)
