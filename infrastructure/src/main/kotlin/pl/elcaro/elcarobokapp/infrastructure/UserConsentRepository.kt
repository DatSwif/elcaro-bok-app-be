package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.UserConsent
import pl.elcaro.elcarobokapp.domain.registration.UserConsentId

interface UserConsentRepository : CrudRepository<UserConsent, UserConsentId> {
    fun findByClient(client: Client): List<UserConsent>
}
