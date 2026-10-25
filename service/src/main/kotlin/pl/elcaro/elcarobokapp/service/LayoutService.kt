package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.PersonalMessageRepository

@Service
@Transactional
class LayoutService(
    private val clientRepository: ClientRepository,
    private val personalMessageRepository: PersonalMessageRepository
) {

    @Transactional(readOnly = true)
    fun getLayoutData(clientNumber: String): LayoutResponse {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        val messageCount = personalMessageRepository.findAllByClientClientNumberOrderByIdDesc(clientNumber).count { !it.isRead }

        return LayoutResponse(
            firstName = client.firstName ?: "",
            lastName = client.lastName ?: "",
            messageCount = messageCount,
            languageCode = client.languageCode
        )
    }
}
