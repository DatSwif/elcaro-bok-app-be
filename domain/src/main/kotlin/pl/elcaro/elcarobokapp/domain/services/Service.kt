package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "services")
class Service(
    @Column(precision = 10, scale = 2)
    var monthlyFee: BigDecimal? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "services_hardware",
        joinColumns = [JoinColumn(name = "service_id")],
        inverseJoinColumns = [JoinColumn(name = "hardware_id")]
    )
    var hardware: MutableSet<Hardware> = mutableSetOf()

    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableSet<ServiceTranslation> = mutableSetOf()
}
