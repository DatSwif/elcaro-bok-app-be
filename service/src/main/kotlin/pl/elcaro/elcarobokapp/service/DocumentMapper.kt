package pl.elcaro.elcarobokapp.service

import pl.elcaro.elcarobokapp.domain.Document
import pl.elcaro.elcarobokapp.domain.DocumentTranslation
import java.util.*

fun Document.toView(locale: Locale): DocumentView {
    val translation = findTranslation(locale)
    return DocumentView(id = id, name = translation?.name ?: "", text = translation?.text ?: "")
}

fun Document.toSummary(locale: Locale): DocumentSummary {
    val translation = findTranslation(locale)
    return DocumentSummary(id = id, name = translation?.name ?: "")
}

private fun Document.findTranslation(locale: Locale): DocumentTranslation? =
    translations.firstOrNull { it.id.languageCode == locale.language }
        ?: translations.firstOrNull { it.id.languageCode == "pl" }
        ?: translations.firstOrNull()
