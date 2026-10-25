package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.messages.ClientGlobalMessageState
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessage
import pl.elcaro.elcarobokapp.domain.messages.PersonalMessage

interface GlobalMessageRepository : CrudRepository<GlobalMessage, Long> {
    fun findAllByOrderByIdDesc(): List<GlobalMessage>
}

interface ClientGlobalMessageStateRepository : CrudRepository<ClientGlobalMessageState, Long> {
    fun findByClientClientNumberAndGlobalMessageId(
        clientNumber: String,
        globalMessageId: Long,
    ): ClientGlobalMessageState?

    fun findAllByClientClientNumber(clientNumber: String): List<ClientGlobalMessageState>
}

interface PersonalMessageRepository : CrudRepository<PersonalMessage, Long> {
    fun findAllByClientClientNumberOrderByIdDesc(clientNumber: String): List<PersonalMessage>
    fun findByIdAndClientClientNumber(id: Long, clientNumber: String): PersonalMessage?
    fun countByClientClientNumber(clientNumber: String): Int
}
