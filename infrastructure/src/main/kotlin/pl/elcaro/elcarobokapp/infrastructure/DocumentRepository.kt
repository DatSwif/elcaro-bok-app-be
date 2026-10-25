package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import pl.elcaro.elcarobokapp.domain.Document

interface DocumentRepository : JpaRepository<Document, String> {

    @EntityGraph(attributePaths = ["translations"])
    override fun findAll(): List<Document>
}
