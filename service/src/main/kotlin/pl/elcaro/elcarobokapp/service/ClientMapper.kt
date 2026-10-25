package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Component
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientEmail
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.infrastructure.ClientEmailRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import java.time.Instant

@Component
class ClientMapper(
    private val clientPhoneRepository: ClientPhoneRepository,
    private val clientEmailRepository: ClientEmailRepository,
) {
    fun applyUpdate(request: ClientUpdateRequest, client: Client) {
        request.firstName?.let { client.firstName = it }
        request.lastName?.let { client.lastName = it }
        request.organizationName?.let { client.organizationName = it }
        request.languageCode?.let { client.languageCode = it }

        request.emails?.let { incomingEmails ->
            val currentEmails = client.emails.map { it.email }.toSet()
            val newEmails = incomingEmails.toSet()

            client.emails.removeIf { it.email !in newEmails }

            val clientId = client.id
            (newEmails - currentEmails).forEach { email ->
                if (clientId != null && clientEmailRepository.existsByEmailAndClientIdNot(email, clientId)) {
                    throw DuplicateContactException(ContactField.EMAIL, email)
                }
                client.emails.add(ClientEmail(client = client, email = email, createdAt = Instant.now()))
            }
        }

        request.phones?.let { incomingPhones ->
            val currentPhones = client.phones.map { it.phone }.toSet()
            val newPhones = incomingPhones.toSet()

            client.phones.removeIf { it.phone !in newPhones }

            val clientId = client.id
            (newPhones - currentPhones).forEach { phone ->
                if (clientId != null && clientPhoneRepository.existsByPhoneAndClientIdNot(phone, clientId)) {
                    throw DuplicateContactException(ContactField.PHONE, phone)
                }
                client.phones.add(
                    ClientPhone(
                        client = client,
                        phone = phone,
                        expiresAt = Instant.now().plusSeconds(3600),
                        createdAt = Instant.now(),
                    )
                )
            }
        }
    }
}

fun Client.toResponse() = ClientResponse(
    id = id!!,
    clientNumber = clientNumber,
    firstName = firstName,
    lastName = lastName,
    organizationName = organizationName,
    languageCode = languageCode,
    emails = emails.map { it.email },
    phones = phones.map { it.phone },
)
