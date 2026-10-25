package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.elcaro.elcarobokapp.domain.services.ClientHardware
import java.util.*

interface ClientHardwareRepository : JpaRepository<ClientHardware, Long> {

    @EntityGraph(attributePaths = ["hardware", "hardware.translations", "clientService"])
    @Query("SELECT ch FROM ClientHardware ch WHERE ch.clientService.client.clientNumber = :clientNumber")
    fun findAllByClientNumber(clientNumber: String): List<ClientHardware>

    @EntityGraph(attributePaths = ["hardware", "hardware.translations", "clientService"])
    @Query("SELECT ch FROM ClientHardware ch WHERE ch.id = :id AND ch.clientService.client.clientNumber = :clientNumber")
    fun findByIdAndClientNumber(id: Long, clientNumber: String): Optional<ClientHardware>
}
