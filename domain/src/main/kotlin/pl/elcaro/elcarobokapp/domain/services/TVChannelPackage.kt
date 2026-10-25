package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "tv_channel_packages")
class TVChannelPackage(
    @Column(name = "service_id", nullable = false)
    var serviceId: Long,

    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    var monthlyFee: BigDecimal,

    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Column(name = "is_base_package", nullable = false)
    var isBasePackage: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tv_channel_offers",
        joinColumns = [
            JoinColumn(name = "tv_service_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "tv_channel_id")
        ]
    )
    var channels: MutableSet<TVChannel> = mutableSetOf()

    @OneToMany(mappedBy = "tvChannelPackage", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableSet<TVChannelPackageTranslation> = mutableSetOf()
}
