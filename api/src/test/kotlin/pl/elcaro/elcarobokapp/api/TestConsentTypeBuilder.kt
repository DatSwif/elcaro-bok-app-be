package pl.elcaro.elcarobokapp.api

import pl.elcaro.elcarobokapp.domain.registration.ConsentType
import pl.elcaro.elcarobokapp.domain.registration.ConsentTypeTranslation
import pl.elcaro.elcarobokapp.domain.registration.ConsentTypeTranslationId

fun testConsentTypeWithTranslations(
    enCode: String = "MARKETING",
    plCode: String = "MARKETING",
    enTitle: String = "Marketing consent",
    plTitle: String = "Zgoda marketingowa",
    enDescription: String = "Marketing communications consent",
    plDescription: String = "Zgoda na komunikację marketingową",
): ConsentType {
    val type = ConsentType()
    type.translations = mutableListOf(
        ConsentTypeTranslation(
            id = ConsentTypeTranslationId("en", 0),
            consentType = type,
            code = enCode,
            title = enTitle,
            description = enDescription,
        ),
        ConsentTypeTranslation(
            id = ConsentTypeTranslationId("pl", 0),
            consentType = type,
            code = plCode,
            title = plTitle,
            description = plDescription,
        ),
    )
    return type
}
