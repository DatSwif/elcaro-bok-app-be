package pl.elcaro.elcarobokapp.domain.messages

import jakarta.persistence.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.time.Instant

@Entity
@Table(
    name = "client_global_message_states",
    uniqueConstraints = [UniqueConstraint(
        name = "uc_client_global_message_states",
        columnNames = ["client_id", "global_message_id"]
    )]
)
class ClientGlobalMessageState(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "global_message_id", nullable = false)
    var globalMessage: GlobalMessage,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
