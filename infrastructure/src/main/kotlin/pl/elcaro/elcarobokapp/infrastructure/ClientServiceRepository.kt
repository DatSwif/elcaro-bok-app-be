package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.elcaro.elcarobokapp.domain.services.ClientService
import java.util.*

interface ClientServiceRepository : JpaRepository<ClientService, Long> {

    @EntityGraph(attributePaths = ["service", "service.translations", "clientAddress"])
    @Query("""
        SELECT cs FROM ClientService cs
        WHERE cs.client.clientNumber = :clientNumber
    """)
    fun findAllByClientNumber(clientNumber: String): List<ClientService>

    @EntityGraph(attributePaths = ["service", "service.translations", "service.hardware", "service.hardware.translations", "clientAddress"])
    @Query("""
        SELECT cs FROM ClientService cs
        WHERE cs.id = :id AND cs.client.clientNumber = :clientNumber
    """)
    fun findByIdAndClientNumber(id: Long, clientNumber: String): Optional<ClientService>

    fun existsByClientClientNumberAndServiceId(clientNumber: String, serviceId: Long): Boolean
}
