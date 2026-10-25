package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.registration.UserConsent
import pl.elcaro.elcarobokapp.domain.registration.UserConsentId
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ConsentTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import java.time.Instant
import java.util.*

@Service
@Transactional
class ConsentService(
    private val clientRepository: ClientRepository,
    private val consentTypeRepository: ConsentTypeRepository,
    private val userConsentRepository: UserConsentRepository
) {

    @Transactional(readOnly = true)
    fun getClientConsents(clientNumber: String, locale: Locale): List<MarketingConsentResponse> {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        val allConsentTypes = consentTypeRepository.findAll()
        val userConsents = userConsentRepository.findByClient(client).associateBy { it.consentType.id }

        return allConsentTypes.map { type ->
            val userConsent = userConsents[type.id]

            val translation = type.translations.firstOrNull { it.id.languageCode == locale.language }
                ?: type.translations.firstOrNull { it.id.languageCode == "pl" }
                ?: type.translations.firstOrNull()

            MarketingConsentResponse(
                code = translation?.code ?: "UNKNOWN",
                title = translation?.title ?: translation?.code ?: "UNKNOWN",
                description = translation?.description ?: "",
                isGranted = userConsent?.isGranted ?: false,
                isEditable = true,
                grantedAt = userConsent?.grantedAt
            )
        }
    }

    fun updateConsent(clientNumber: String, request: ConsentUpdateRequest) {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        val consentType = consentTypeRepository.findByTranslationsCode(request.code)
            ?: throw IllegalArgumentException("Consent type not found for code: ${request.code}")

        val consentId = UserConsentId(clientId = client.id, consentTypeId = consentType.id)
        val existingConsent = userConsentRepository.findById(consentId).orElse(null)

        if (existingConsent != null) {
            existingConsent.isGranted = request.isGranted
            existingConsent.grantedAt = Instant.now()
            userConsentRepository.save(existingConsent)
        } else {
            userConsentRepository.save(
                UserConsent(
                    id = consentId,
                    client = client,
                    consentType = consentType,
                    isGranted = request.isGranted,
                    grantedAt = Instant.now()
                )
            )
        }
    }
}
