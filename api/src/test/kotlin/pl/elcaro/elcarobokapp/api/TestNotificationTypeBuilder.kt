package pl.elcaro.elcarobokapp.api

import pl.elcaro.elcarobokapp.domain.registration.NotificationType
import pl.elcaro.elcarobokapp.domain.registration.NotificationTypeTranslation
import pl.elcaro.elcarobokapp.domain.registration.NotificationTypeTranslationId

fun testNotificationTypeWithTranslations(
    code: String = "EMAIL_PROMO",
    enName: String = "Promotional emails",
    plName: String = "Emaile promocyjne",
): NotificationType {
    val type = NotificationType(code = code)
    type.translations = mutableListOf(
        NotificationTypeTranslation(
            id = NotificationTypeTranslationId("en", 0),
            notificationType = type,
            name = enName,
        ),
        NotificationTypeTranslation(
            id = NotificationTypeTranslationId("pl", 0),
            notificationType = type,
            name = plName,
        ),
    )
    return type
}
