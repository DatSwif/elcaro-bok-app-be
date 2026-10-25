package pl.elcaro.elcarobokapp.domain.messages

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.time.Instant

@Entity
@Table(name = "personal_messages")
class PersonalMessage(
    @Column(nullable = false, length = 1000)
    var title: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String? = null,

    @Column(nullable = false)
    var level: Int,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}