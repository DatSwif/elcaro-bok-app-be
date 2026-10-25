package pl.elcaro.elcarobokapp.service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository

@Service
@Transactional
class ClientService(
    private val clientRepository: ClientRepository,
    private val clientMapper: ClientMapper,
) {
    @Transactional(readOnly = true)
    fun getClientByClientNumber(clientNumber: String): ClientResponse =
        findClientOrThrow(clientNumber).toResponse()

    @Transactional(readOnly = true)
    fun getPhonesByClientNumber(clientNumber: String): List<String> =
        findClientOrThrow(clientNumber).phones.map { it.phone }

    @Transactional(readOnly = true)
    fun getEmailsByClientNumber(clientNumber: String): List<String> =
        findClientOrThrow(clientNumber).emails.map { it.email }

    @CacheEvict(value = ["clientLanguage"], key = "#clientNumber")
    fun updateClientByClientNumber(clientNumber: String, request: ClientUpdateRequest): ClientResponse {
        val existingClient = findClientOrThrow(clientNumber)
        clientMapper.applyUpdate(request, existingClient)
        return clientRepository.save(existingClient).toResponse()
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ["clientLanguage"], key = "#clientNumber")
    fun getLanguageCode(clientNumber: String): String? =
        clientRepository.findLanguageCodeByClientNumber(clientNumber)

    private fun findClientOrThrow(clientNumber: String): Client =
        clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client with number $clientNumber not found")
}
