package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import java.util.*

@Service
class SupportService {

    private val categories = mapOf(
        "pl" to listOf(
            FaqCategoryResponse("1", "assets/icons/fi-rr-credit-card-2.svg", "Faktury i płatności", "billing"),
            FaqCategoryResponse("2", "assets/icons/fi-rr-question.svg", "Pomoc techniczna", "support"),
            FaqCategoryResponse("3", "assets/icons/fi-rr-document-signed.svg", "Moja umowa", "contract"),
            FaqCategoryResponse("4", "assets/icons/fi-rr-internet.svg", "Usługi i pakiety", "services-and-packages"),
            FaqCategoryResponse("5", "assets/icons/fi-rr-settings-sliders.svg", "Zarządzanie kontem", "account"),
            FaqCategoryResponse("6", "assets/icons/fi-rr-browser.svg", "Pozostałe", "other")
        ),
        "en" to listOf(
            FaqCategoryResponse("1", "assets/icons/fi-rr-credit-card-2.svg", "Billing and payments", "billing"),
            FaqCategoryResponse("2", "assets/icons/fi-rr-question.svg", "Technical support", "support"),
            FaqCategoryResponse("3", "assets/icons/fi-rr-document-signed.svg", "My contract", "contract"),
            FaqCategoryResponse("4", "assets/icons/fi-rr-internet.svg", "Services and packages", "services-and-packages"),
            FaqCategoryResponse("5", "assets/icons/fi-rr-settings-sliders.svg", "Account management", "account"),
            FaqCategoryResponse("6", "assets/icons/fi-rr-browser.svg", "Other", "other")
        )
    )

    private val questions = mapOf(
        "pl" to mapOf(
            "billing" to listOf(
                FaqItem("q1", "Pierwsza faktura po zawarciu lub zmianie umowy", "Pierwsza faktura zostanie przesłana w terminie 14 dni roboczych od dnia wykonania instalacji. Faktura zostanie wystawiona w formie elektronicznej bądź tradycyjnej zgodnie ze złożoną dyspozycją podczas podpisywania umowy.", "Faktury"),
                FaqItem("q2", "Jakie płatności są obsługiwane przez aplikację.", "Aplikacja obsługuje płatności BLIK, karty płatnicze (Visa/Mastercard) oraz szybkie przelewy online.", "Faktury"),
            ),
            "support" to listOf(
                FaqItem("s1", "Brak internetu", "Sprawdź podłączenie routera do prądu.", "Techniczne")
            ),
            "contract" to listOf(
                FaqItem("c1", "Jak przedłużyć umowę?", "Umowę można przedłużyć w panelu klienta lub kontaktując się z biurem obsługi.", "Umowa")
            ),
            "services-and-packages" to listOf(
                FaqItem("sp1", "Jakie pakiety TV są dostępne?", "Pełna lista pakietów TV jest dostępna w zakładce 'Usługi'.", "Pakiety")
            ),
            "account" to listOf(
                FaqItem("a1", "Jak zalogować się do aplikacji?", "Logowanie odbywa się za pomocą jednorazowego kodu PIN wysyłanego SMS-em na Twój numer telefonu. Nie musisz pamiętać hasła!", "Konto")
            ),
            "other" to listOf(
                FaqItem("o1", "Gdzie znajdę regulaminy?", "Regulaminy są dostępne w sekcji 'Dokumenty'.", "Inne")
            )
        ),
        "en" to mapOf(
            "billing" to listOf(
                FaqItem("q1", "First invoice after signing or changing the contract", "The first invoice will be sent within 14 business days from the date of installation. The invoice will be issued in electronic or traditional form according to the instruction given when signing the contract.", "Invoices"),
                FaqItem("q2", "What payments are supported by the application.", "The application supports BLIK, payment cards (Visa/Mastercard), and fast online bank transfers.", "Invoices"),
            ),
            "support" to listOf(
                FaqItem("s1", "No internet", "Check if the router is plugged in.", "Technical")
            ),
            "contract" to listOf(
                FaqItem("c1", "How to extend the contract?", "You can extend your contract in the client panel or by contacting customer service.", "Contract")
            ),
            "services-and-packages" to listOf(
                FaqItem("sp1", "What TV packages are available?", "A full list of TV packages is available in the 'Services' tab.", "Packages")
            ),
            "account" to listOf(
                FaqItem("a1", "How to log in to the application?", "Login is done via a one-time PIN code sent by SMS to your phone number. You don't need to remember a password!", "Account")
            ),
            "other" to listOf(
                FaqItem("o1", "Where can I find the regulations?", "The regulations are available in the 'Documents' section.", "Other")
            )
        )
    )

    fun getContactHelp(locale: Locale): ContactHelpResponse {
        val days = if (locale.language == "pl") "Pn-Nd" else "Mon-Sun"
        return ContactHelpResponse(
            phone = PhoneDto(
                number = "409 901 990",
                schedule = ScheduleDto(
                    days = days,
                    hours = "8:00-22:00"
                )
            ),
            email = EmailDto(
                address = "info@elcaroco.pl",
                schedule = ScheduleDto(
                    days = days,
                    hours = "24/7"
                )
            )
        )
    }

    fun getFaqCategories(locale: Locale): List<FaqCategoryResponse> {
        val lang = if (locale.language == "pl") "pl" else "en"
        return categories[lang] ?: categories["pl"]!!
    }

    fun getFaqCategoryByRoute(route: String, locale: Locale): FaqPageResponse {
        val lang = if (locale.language == "pl") "pl" else "en"
        val categoryItems = questions[lang]?.get(route)
            ?: throw NoSuchElementException("Category route not found: $route")

        val categoryTitle = categories[lang]?.find { it.route == route }?.title ?: "Unknown"

        return FaqPageResponse(
            id = route,
            title = categoryTitle,
            content = categoryItems
        )
    }

    fun searchFaq(query: String, locale: Locale): List<FaqItem> {
        val lang = if (locale.language == "pl") "pl" else "en"
        val allQuestionsForLang = questions[lang]?.values?.flatten() ?: emptyList()
        val lowerQuery = query.lowercase(locale)
        
        return allQuestionsForLang.filter {
            it.title.lowercase(locale).contains(lowerQuery) || 
            it.answer.lowercase(locale).contains(lowerQuery)
        }
    }
}
