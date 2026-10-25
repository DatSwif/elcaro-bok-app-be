package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.registration.Client

interface ClientRepository: CrudRepository<Client, Long> {
    fun findByClientNumber(clientNumber: String): Client?

    @EntityGraph(attributePaths = ["phones"])
    fun findByClientNumberAndPhonesPhoneAndEmailsEmail(clientNumber: String, phone: String, email: String): Client?

    @Query("SELECT c.languageCode FROM Client c WHERE c.clientNumber = :clientNumber")
    fun findLanguageCodeByClientNumber(clientNumber: String): String?
}