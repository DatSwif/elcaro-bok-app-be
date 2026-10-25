package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import java.util.*

@Service
class GroupedDocumentService {

    fun getTermsOfService(locale: Locale): List<DocumentGroupResponse> {
        val title1 = if (locale.language == "pl") "Regulamin świadczenia usług telekomunikacyjnych" else "Terms of telecommunications services"
        val title2 = if (locale.language == "pl") "Regulamin promocji \"Oferta na bloki_lato 2024\"" else "Promotion terms \"Blocks offer_summer 2024\""
        val title3 = if (locale.language == "pl") "Specyfikacja techniczna usług" else "Technical specification of services"
        val label1 = if (locale.language == "pl") "Usługi telekomunikacyjne" else "Telecommunications services"
        val label2 = if (locale.language == "pl") "Inne" else "Other"

        return listOf(
            DocumentGroupResponse(
                label = label1,
                type = "telecommunications-services",
                documents = listOf(
                    DocumentMetadataResponse("d1", title1, "telecommunications-services"),
                    DocumentMetadataResponse("d3", title2, "telecommunications-services")
                )
            ),
            DocumentGroupResponse(
                label = label2,
                type = "other",
                documents = listOf(
                    DocumentMetadataResponse("s1", title3, "other")
                )
            )
        )
    }

    fun getPrivacyPolicy(locale: Locale): List<DocumentGroupResponse> {
        val title1 = if (locale.language == "pl") "Polityka prywatności" else "Privacy policy"
        val title2 = if (locale.language == "pl") "Zgoda marketingowa" else "Marketing consent"
        val label1 = if (locale.language == "pl") "Polityka prywatności" else "Privacy policy"
        val label2 = if (locale.language == "pl") "Zgody" else "Consents"

        return listOf(
            DocumentGroupResponse(
                label = label1,
                type = "privacy",
                documents = listOf(
                    DocumentMetadataResponse("p1", title1, "privacy")
                )
            ),
            DocumentGroupResponse(
                label = label2,
                type = "consents",
                documents = listOf(
                    DocumentMetadataResponse("p2", title2, "consents")
                )
            )
        )
    }
}
