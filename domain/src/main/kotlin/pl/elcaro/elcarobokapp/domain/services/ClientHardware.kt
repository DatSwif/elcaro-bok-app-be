package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*

@Entity
@Table(name = "client_hardware")
class ClientHardware(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hardware_id", nullable = false)
    var hardware: Hardware,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_service_id", nullable = false)
    var clientService: ClientService,

    @Column(nullable = false, length = 50)
    var ownership: String,

    @Column(name = "serial_number", length = 100)
    var serialNumber: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}