package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientEmailRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository

class ClientServiceTest : DescribeSpec({

    val clientRepository = mockk<ClientRepository>()
    val clientPhoneRepository = mockk<ClientPhoneRepository>()
    val clientEmailRepository = mockk<ClientEmailRepository>()
    val clientMapper = ClientMapper(clientPhoneRepository, clientEmailRepository)
    val clientService = ClientService(clientRepository, clientMapper)

    beforeEach { clearAllMocks() }

    fun client(id: Long = 1L, clientNumber: String = "C001") = Client(
        clientNumber = clientNumber,
        firstName = "Jan",
        lastName = "Kowalski",
        languageCode = "pl",
    ).also { it.id = id }

    describe("getClientByClientNumber") {

        it("returns the client response when found") {
            every { clientRepository.findByClientNumber("C001") } returns client(1L)

            val result = clientService.getClientByClientNumber("C001")

            result.id shouldBe 1L
            result.clientNumber shouldBe "C001"
            result.firstName shouldBe "Jan"
            result.languageCode shouldBe "pl"
        }

        it("throws NoSuchElementException when client does not exist") {
            every { clientRepository.findByClientNumber("X") } returns null

            val ex = shouldThrow<NoSuchElementException> { clientService.getClientByClientNumber("X") }
            ex.message shouldBe "Client with number X not found"
        }
    }

    describe("updateClientByClientNumber") {

        it("applies partial updates and returns the modified client") {
            val existing = client(1L)
            every { clientRepository.findByClientNumber("C001") } returns existing
            every { clientRepository.save(existing) } returns existing

            val request = ClientUpdateRequest(firstName = "Piotr", languageCode = "en")
            val result = clientService.updateClientByClientNumber("C001", request)

            result.firstName shouldBe "Piotr"
            result.languageCode shouldBe "en"
            verify(exactly = 1) { clientRepository.save(existing) }
        }

        it("only updates provided fields, leaving others unchanged") {
            val existing = client(1L).also { it.organizationName = "Elcaro" }
            every { clientRepository.findByClientNumber("C001") } returns existing
            every { clientRepository.save(existing) } returns existing

            val request = ClientUpdateRequest(firstName = "Piotr")
            val result = clientService.updateClientByClientNumber("C001", request)

            result.firstName shouldBe "Piotr"
            result.languageCode shouldBe "pl"
            result.organizationName shouldBe "Elcaro"
        }

        it("throws NoSuchElementException when client does not exist") {
            every { clientRepository.findByClientNumber("X") } returns null

            val ex = shouldThrow<NoSuchElementException> {
                clientService.updateClientByClientNumber("X", ClientUpdateRequest(firstName = "X"))
            }
            ex.message shouldBe "Client with number X not found"
        }
    }

    describe("getLanguageCode") {

        it("returns the language code for an existing client") {
            every { clientRepository.findLanguageCodeByClientNumber("C001") } returns "pl"

            clientService.getLanguageCode("C001") shouldBe "pl"
        }

        it("returns null when client does not exist") {
            every { clientRepository.findLanguageCodeByClientNumber("X") } returns null

            clientService.getLanguageCode("X") shouldBe null
        }
    }
})
