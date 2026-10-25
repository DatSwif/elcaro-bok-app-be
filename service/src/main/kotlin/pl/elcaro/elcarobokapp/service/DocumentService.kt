package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.infrastructure.DocumentRepository
import java.util.*

@Service
@Transactional(readOnly = true)
class DocumentService(
    private val documentRepository: DocumentRepository,
) {
    fun getAllDocuments(locale: Locale): List<DocumentSummary> =
        documentRepository.findAll().map { it.toSummary(locale) }

    fun getDocumentById(id: String, locale: Locale): DocumentView =
        documentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Document with id $id not found") }
            .toView(locale)
}

data class DocumentView(
    val id: String,
    val name: String,
    val text: String,
)

data class DocumentSummary(
    val id: String,
    val name: String,
)
