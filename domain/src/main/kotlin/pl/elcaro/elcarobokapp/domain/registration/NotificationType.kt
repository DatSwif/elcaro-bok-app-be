package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*

@Entity
@Table(name = "notification_types")
class NotificationType(
    @Column(length = 50, nullable = false)
    var code: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null

    @OneToMany(mappedBy = "notificationType", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableList<NotificationTypeTranslation> = mutableListOf()
}
