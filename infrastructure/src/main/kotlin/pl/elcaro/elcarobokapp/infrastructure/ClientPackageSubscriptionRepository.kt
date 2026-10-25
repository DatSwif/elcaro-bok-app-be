package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pl.elcaro.elcarobokapp.domain.services.ClientPackageSubscription

interface ClientPackageSubscriptionRepository : JpaRepository<ClientPackageSubscription, Long> {
    fun existsByClientNumberAndTvChannelPackageId(clientNumber: String, tvChannelPackageId: Long): Boolean
    fun deleteByClientNumberAndTvChannelPackageId(clientNumber: String, tvChannelPackageId: Long)
    fun findAllByClientNumberAndServiceId(clientNumber: String, serviceId: Long): List<ClientPackageSubscription>
}
