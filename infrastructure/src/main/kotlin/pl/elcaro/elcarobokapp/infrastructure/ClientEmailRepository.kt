package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.registration.ClientEmail

interface ClientEmailRepository : CrudRepository<ClientEmail, Int> {
    fun findByEmail(email: String): ClientEmail?

    fun existsByEmailAndClientIdNot(email: String, clientId: Long): Boolean
}
