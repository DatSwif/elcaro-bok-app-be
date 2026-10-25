package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.infrastructure.*
import java.util.*

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val clientRepository: ClientRepository,
    private val globalMessageRepository: GlobalMessageRepository,
    private val clientServiceRepository: ClientServiceRepository,
    private val offerRepository: OfferRepository,
    private val billingService: BillingService,
    private val clientGlobalMessageStateRepository: ClientGlobalMessageStateRepository
) {

    fun getDashboardData(clientNumber: String, locale: java.util.Locale): DashboardResponse {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        // 1. Account (from BillingService)
        val billingSummary = billingService.getBillingSummary(clientNumber)
        val account = DashboardAccountDto(
            accountNumber = billingSummary.accountNumber,
            balance = billingSummary.balance
        )

        // 2. Messages (Unread global messages)
        val clientMessageStates = clientGlobalMessageStateRepository.findAllByClientClientNumber(clientNumber)
            .associateBy { it.globalMessage.id }
            
        val messages = globalMessageRepository.findAll()
            .sortedByDescending { it.id }
            .filter { message ->
                val state = clientMessageStates[message.id]
                state == null || !state.isRead
            }
            .map { message ->
                val translation = message.translations.firstOrNull { it.id.languageCode == locale.language }
                    ?: message.translations.firstOrNull { it.id.languageCode == "pl" }
                    ?: message.translations.firstOrNull()

                DashboardMessageDto(
                    id = message.id!!,
                    title = translation?.title ?: "No title",
                    content = translation?.content ?: "No content"
                )
            }

        // 3. Services (All count grouped by type)
        val clientServices = clientServiceRepository.findAll().filter { it.client?.id == client.id }
        val services = clientServices
            .groupBy { it.serviceType ?: "OTHER" }
            .map { (type, servicesList) ->
                DashboardServiceDto(
                    serviceType = type,
                    activeCount = servicesList.size
                )
            }

        // 4. Offers (2 random available offers)
        val offers = offerRepository.findAll()
            .shuffled()
            .take(2)
            .map { offer ->
                val translation = offer.translations.firstOrNull { it.id.languageCode == locale.language }
                    ?: offer.translations.firstOrNull { it.id.languageCode == "pl" }
                    ?: offer.translations.firstOrNull()

                DashboardOfferDto(
                    id = offer.id!!,
                    title = translation?.title ?: "Oferta",
                    subtitle = translation?.subtitle ?: "",
                    imageUrl = offer.imageUrl
                )
            }

        return DashboardResponse(
            account = account,
            messages = messages,
            services = services,
            offers = offers
        )
    }
}
