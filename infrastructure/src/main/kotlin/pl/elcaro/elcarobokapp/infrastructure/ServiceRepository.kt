package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.elcaro.elcarobokapp.domain.services.Service
import java.util.*

interface ServiceRepository : JpaRepository<Service, Long> {

    @EntityGraph(attributePaths = ["translations", "hardware", "hardware.translations"])
    @Query("SELECT s FROM Service s JOIN ClientService cs ON cs.service = s WHERE s.id = :serviceId AND cs.client.clientNumber = :clientNumber")
    fun findByIdAndClientNumber(serviceId: Long, clientNumber: String): Optional<Service>

    @EntityGraph(attributePaths = ["translations", "hardware", "hardware.translations"])
    @Query("""
        SELECT s FROM Service s
        JOIN ClientService cs ON cs.service = s
        WHERE cs.client.clientNumber = :clientNumber
    """)
    fun findAllByClientNumber(clientNumber: String): List<Service>
}
