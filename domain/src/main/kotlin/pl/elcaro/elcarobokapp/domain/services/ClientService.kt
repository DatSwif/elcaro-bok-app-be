package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "client_services")
class ClientService(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    var service: Service,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    var client: Client,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    var clientAddress: ClientAddress? = null,

    @Column(name = "service_type", length = 30)
    var serviceType: String? = null,

    @Column(nullable = false, length = 30)
    var status: String,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "next_payment")
    var nextPayment: Instant? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
