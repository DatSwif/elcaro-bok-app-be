package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pl.elcaro.elcarobokapp.domain.services.ClientAddress

interface ClientAddressRepository : JpaRepository<ClientAddress, Long> {

    fun findAllByClientClientNumber(clientNumber: String): List<ClientAddress>
}
