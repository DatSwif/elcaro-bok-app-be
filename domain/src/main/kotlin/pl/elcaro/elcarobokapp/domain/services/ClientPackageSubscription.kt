package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*

@Entity
@Table(
    name = "client_package_subscriptions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["client_number", "tv_channel_package_id"])],
)
class ClientPackageSubscription(
    @Column(name = "client_number", nullable = false, length = 50) val clientNumber: String,
    @Column(name = "tv_channel_package_id", nullable = false) val tvChannelPackageId: Long,
    @Column(name = "service_id", nullable = false) val serviceId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
