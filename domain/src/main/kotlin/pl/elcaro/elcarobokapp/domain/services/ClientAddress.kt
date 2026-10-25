package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.time.LocalDate

@Entity
@Table(name = "client_addresses")
class ClientAddress(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    var client: Client,

    @Column(nullable = false, length = 200)
    var address: String,

    @Column(name = "contract_active_from", nullable = false)
    var contractActiveFrom: LocalDate,

    @Column(name = "contract_active_until")
    var contractActiveUntil: LocalDate? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
