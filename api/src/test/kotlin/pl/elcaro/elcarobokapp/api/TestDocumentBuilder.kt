package pl.elcaro.elcarobokapp.api

import pl.elcaro.elcarobokapp.domain.Document
import pl.elcaro.elcarobokapp.domain.DocumentTranslation
import pl.elcaro.elcarobokapp.domain.DocumentTranslationId

fun testDocumentWithTranslations(id: String = "Doc1"): Document {
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
