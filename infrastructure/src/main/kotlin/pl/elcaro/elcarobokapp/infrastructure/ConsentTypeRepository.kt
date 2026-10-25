package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.registration.ConsentType

interface ConsentTypeRepository : CrudRepository<ConsentType, Int> {
    @EntityGraph(attributePaths = ["translations"])
    override fun findAll(): Iterable<ConsentType>

    fun findByTranslationsCode(code: String): ConsentType?
}
