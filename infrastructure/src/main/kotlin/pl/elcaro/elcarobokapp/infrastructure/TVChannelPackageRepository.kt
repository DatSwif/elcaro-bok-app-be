package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.elcaro.elcarobokapp.domain.services.TVChannelPackage
import java.util.*

interface TVChannelPackageRepository : JpaRepository<TVChannelPackage, Long> {

    @EntityGraph(attributePaths = ["translations", "channels", "channels.translations"])
    fun findAllByServiceId(serviceId: Long): List<TVChannelPackage>

    @EntityGraph(attributePaths = ["translations", "channels", "channels.translations"])
    override fun findById(id: Long): Optional<TVChannelPackage>

    @EntityGraph(attributePaths = ["translations", "channels", "channels.translations"])
    @Query("""
        SELECT p FROM TVChannelPackage p
        JOIN ClientService cs ON cs.service.id = p.serviceId
        WHERE p.id = :packageId AND p.serviceId = :serviceId AND cs.client.clientNumber = :clientNumber
    """)
    fun findByIdAndServiceIdAndClientNumber(packageId: Long, serviceId: Long, clientNumber: String): Optional<TVChannelPackage>
}
