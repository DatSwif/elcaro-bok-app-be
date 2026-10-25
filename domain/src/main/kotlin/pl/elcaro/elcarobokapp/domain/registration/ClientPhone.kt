package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "client_phones")
class ClientPhone(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @Column(unique = true, nullable = false, length = 20)
    var phone: String,

    @Column(length = 20)
    var code: String? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
}