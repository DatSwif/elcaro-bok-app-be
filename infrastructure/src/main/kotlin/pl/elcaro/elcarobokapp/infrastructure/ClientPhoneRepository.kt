package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone

interface ClientPhoneRepository : CrudRepository<ClientPhone, Int> {
    @EntityGraph(attributePaths = ["client"])
    fun findByPhone(phone: String): ClientPhone?

    fun existsByPhoneAndClientIdNot(phone: String, clientId: Long): Boolean
}
