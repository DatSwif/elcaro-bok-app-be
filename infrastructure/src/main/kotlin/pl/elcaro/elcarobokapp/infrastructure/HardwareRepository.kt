package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.elcaro.elcarobokapp.domain.services.Hardware
import java.util.*

interface HardwareRepository : JpaRepository<Hardware, Long> {

    @EntityGraph(attributePaths = ["translations"])
    @Query("SELECT DISTINCT h FROM ClientService cs JOIN cs.service s JOIN s.hardware h WHERE cs.client.clientNumber = :clientNumber")
    fun findAllByClientNumber(clientNumber: String): List<Hardware>

    @EntityGraph(attributePaths = ["translations"])
    @Query("SELECT DISTINCT h FROM ClientService cs JOIN cs.service s JOIN s.hardware h WHERE h.id = :hardwareId AND cs.client.clientNumber = :clientNumber")
    fun findByIdAndClientNumber(hardwareId: Long, clientNumber: String): Optional<Hardware>
}
