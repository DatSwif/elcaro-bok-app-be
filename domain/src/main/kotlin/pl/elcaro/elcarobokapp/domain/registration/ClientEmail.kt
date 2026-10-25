package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "client_emails")
class ClientEmail(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @Column(unique = true, nullable = false, length = 255)
    var email: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
}