package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*

@Entity
@Table(name = "user_notification_settings")
class UserNotificationSetting(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_type_id", nullable = false)
    var notificationType: NotificationType,

    @Column(length = 20)
    var channel: String? = null,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}