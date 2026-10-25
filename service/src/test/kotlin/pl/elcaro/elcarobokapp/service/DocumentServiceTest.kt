package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import pl.elcaro.elcarobokapp.domain.Document
import pl.elcaro.elcarobokapp.domain.DocumentTranslation
import pl.elcaro.elcarobokapp.domain.DocumentTranslationId
import pl.elcaro.elcarobokapp.infrastructure.DocumentRepository
import java.util.*

class DocumentServiceTest : DescribeSpec({

    val documentRepository = mockk<DocumentRepository>()
    val documentService = DocumentService(documentRepository)

    beforeEach { clearAllMocks() }

    fun document(id: String = "Doc1"): Document {
        val doc = Document(id = id)
        doc.translations = mutableListOf(
            DocumentTranslation(
                id = DocumentTranslationId("en", id),
                document = doc,
                name = "Document 1",
                text = "English content",
            ),
            DocumentTranslation(
                id = DocumentTranslationId("pl", id),
                document = doc,
                name = "Dokument 1",
                text = "Treść po polsku",
            ),
            DocumentTranslation(
                id = DocumentTranslationId("de", id),
                document = doc,
                name = "Dokument 1 DE",
                text = "Deutscher Inhalt",
            ),
        )
        return doc
    }

    describe("getDocumentById") {

        it("returns DocumentView in English for English locale") {
            every { documentRepository.findById("Doc1") } returns Optional.of(document())

            val result = documentService.getDocumentById("Doc1", Locale.ENGLISH)

            result.id shouldBe "Doc1"
            result.name shouldBe "Document 1"
            result.text shouldBe "English content"
        }

        it("returns DocumentView in Polish for Polish locale") {
            every { documentRepository.findById("Doc1") } returns Optional.of(document())

            val result = documentService.getDocumentById("Doc1", Locale.forLanguageTag("pl"))

            result.id shouldBe "Doc1"
            result.name shouldBe "Dokument 1"
            result.text shouldBe "Treść po polsku"
        }

        it("returns DocumentView in German for German locale") {
            every { documentRepository.findById("Doc1") } returns Optional.of(document())

            val result = documentService.getDocumentById("Doc1", Locale.GERMAN)

            result.id shouldBe "Doc1"
            result.name shouldBe "Dokument 1 DE"
            result.text shouldBe "Deutscher Inhalt"
        }

        it("falls back to Polish for unsupported locale") {
            every { documentRepository.findById("Doc1") } returns Optional.of(document())

            val result = documentService.getDocumentById("Doc1", Locale.FRENCH)

            result.name shouldBe "Dokument 1"
        }

        it("throws NoSuchElementException when document does not exist") {
            every { documentRepository.findById("Missing") } returns Optional.empty()

            val ex = shouldThrow<NoSuchElementException> {
                documentService.getDocumentById("Missing", Locale.ENGLISH)
            }
            ex.message shouldBe "Document with id Missing not found"
        }
    }

    describe("getAllDocuments") {

        it("returns all documents mapped to the requested locale") {
            every { documentRepository.findAll() } returns listOf(document("Doc1"), document("Doc2"))

            val result = documentService.getAllDocuments(Locale.forLanguageTag("pl"))

            result.size shouldBe 2
            result[0].name shouldBe "Dokument 1"
            result[1].name shouldBe "Dokument 1"
        }

        it("returns empty list when no documents exist") {
            every { documentRepository.findAll() } returns emptyList()

            val result = documentService.getAllDocuments(Locale.ENGLISH)

            result shouldBe emptyList()
        }
    }
})
