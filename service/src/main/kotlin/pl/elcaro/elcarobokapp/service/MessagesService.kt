package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.messages.ClientGlobalMessageState
import pl.elcaro.elcarobokapp.infrastructure.ClientGlobalMessageStateRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.GlobalMessageRepository
import pl.elcaro.elcarobokapp.infrastructure.PersonalMessageRepository
import java.time.Instant
import java.util.*

@Service
@Transactional
class MessagesService(
    private val globalMessageRepository: GlobalMessageRepository,
    private val clientGlobalMessageStateRepository: ClientGlobalMessageStateRepository,
    private val personalMessageRepository: PersonalMessageRepository,
    private val clientRepository: ClientRepository,
) {

    @Transactional(readOnly = true)
    fun listGlobal(clientNumber: String, locale: Locale): List<GlobalMessageResponse> {
        val statesByMessageId = clientGlobalMessageStateRepository
            .findAllByClientClientNumber(clientNumber)
            .associateBy { it.globalMessage.id }

        return globalMessageRepository.findAllByOrderByIdDesc()
            .mapNotNull { message ->
                val state = statesByMessageId[message.id]
                if (state?.isDeleted == true) {
                    null
                } else {
                    val translation = message.translations.firstOrNull { it.id.languageCode == locale.language }
                        ?: message.translations.firstOrNull { it.id.languageCode == "pl" }
                        ?: message.translations.firstOrNull()

                    GlobalMessageResponse(
                        id = message.id!!,
                        title = translation?.title ?: "No title",
                        content = translation?.content ?: "No content",
                        isRead = state?.isRead ?: false,
                        readAt = state?.readAt,
                    )
                }
            }
    }

    fun markGlobalRead(clientNumber: String, messageId: Long) {
        val state = loadOrInitGlobalState(clientNumber, messageId)
        if (!state.isRead) {
            state.isRead = true
            state.readAt = Instant.now()
        }
        clientGlobalMessageStateRepository.save(state)
    }

    fun deleteGlobal(clientNumber: String, messageId: Long) {
        val state = loadOrInitGlobalState(clientNumber, messageId)
        state.isDeleted = true
        clientGlobalMessageStateRepository.save(state)
    }

    @Transactional(readOnly = true)
    fun listPersonal(clientNumber: String, locale: Locale): List<PersonalMessageResponse> {
        val isEn = locale.language == "en"

        return personalMessageRepository.findAllByClientClientNumberOrderByIdDesc(clientNumber)
            .map { msg ->
                val payloadMap = mutableMapOf<String, String>()
                if (!msg.payload.isNullOrBlank()) {
                    val regex = """"([^"]+)"\s*:\s*"([^"]+)"""".toRegex()
                    regex.findAll(msg.payload!!).forEach { matchResult ->
                        val (key, value) = matchResult.destructured
                        payloadMap[key] = value
                    }
                }

                PersonalMessageResponse(
                    id = msg.id!!,
                    title = buildPersonalMessageText(msg.title, payloadMap, isEn),
                    level = msg.level,
                    isRead = msg.isRead,
                    readAt = msg.readAt,
                )
            }
    }

    fun markPersonalRead(clientNumber: String, messageId: Long) {
        val message = personalMessageRepository.findByIdAndClientClientNumber(messageId, clientNumber)
            ?: throw NoSuchElementException("Message not found")
        if (!message.isRead) {
            message.isRead = true
            message.readAt = Instant.now()
            personalMessageRepository.save(message)
        }
    }

    fun deletePersonal(clientNumber: String, messageId: Long) {
        val message = personalMessageRepository.findByIdAndClientClientNumber(messageId, clientNumber)
            ?: throw NoSuchElementException("Message not found")
        personalMessageRepository.delete(message)
    }

    private fun loadOrInitGlobalState(clientNumber: String, messageId: Long): ClientGlobalMessageState {
        val existing = clientGlobalMessageStateRepository
            .findByClientClientNumberAndGlobalMessageId(clientNumber, messageId)
        if (existing != null) return existing

        val message = globalMessageRepository.findById(messageId).orElseThrow {
            NoSuchElementException("Message not found")
        }
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")

        return ClientGlobalMessageState(
            client = client,
            globalMessage = message,
        )
    }

    private fun buildPersonalMessageText(typeKey: String, payload: Map<String, String>, isEn: Boolean): String {
        val template = when (typeKey) {
            "MAINTENANCE_DATES" -> if (isEn) "Planned maintenance works on {{startDate}} - {{endDate}}."
            else "Planowane są prace remontowe w dniach {{startDate}} - {{endDate}}."
            "SYSTEM_OUTAGE" -> if (isEn) "An outage has occurred! Some services may not work properly. Our technicians are working on it..."
            else "Wystąpiła awaria! Niektóre z usług mogą działać nieprawidłowo lub wcale. Nasi technicy pracują na usunięciem problemu..."
            "NEW_INVOICE" -> if (isEn) "Invoice {{invoiceNumber}} for {{amount}} has been issued."
            else "Wystawiono nową fakturę {{invoiceNumber}} na kwotę {{amount}}."
            else -> typeKey
        }

        var resultText = template
        payload.forEach { (key, value) ->
            resultText = resultText.replace("{{$key}}", value)
        }

        return resultText
    }
}