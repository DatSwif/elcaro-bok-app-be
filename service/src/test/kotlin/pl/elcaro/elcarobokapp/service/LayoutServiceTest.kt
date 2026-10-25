package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import pl.elcaro.elcarobokapp.domain.messages.PersonalMessage
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.PersonalMessageRepository

class LayoutServiceTest : DescribeSpec({

    val clientRepository = mockk<ClientRepository>()
    val personalMessageRepository = mockk<PersonalMessageRepository>()
    val layoutService = LayoutService(clientRepository, personalMessageRepository)

    beforeEach { clearAllMocks() }

    fun client(
        firstName: String? = "Jan",
        lastName: String? = "Kowalski",
        languageCode: String = "pl",
    ) = Client(
        clientNumber = "C001",
        firstName = firstName,
        lastName = lastName,
        languageCode = languageCode,
    ).also { it.id = 1L }

    fun message(read: Boolean) = PersonalMessage(
        title = "t",
        level = 1,
        isRead = read,
        client = client(),
    )

    describe("getLayoutData") {

        it("returns layout data with unread message count") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { personalMessageRepository.findAllByClientClientNumberOrderByIdDesc("C001") } returns
                listOf(message(read = false), message(read = true), message(read = false))

            val result = layoutService.getLayoutData("C001")

            result.firstName shouldBe "Jan"
            result.lastName shouldBe "Kowalski"
            result.languageCode shouldBe "pl"
            result.messageCount shouldBe 2
        }

        it("returns messageCount = 0 when client has no personal messages") {
            every { clientRepository.findByClientNumber("C001") } returns client()
            every { personalMessageRepository.findAllByClientClientNumberOrderByIdDesc("C001") } returns emptyList()

            val result = layoutService.getLayoutData("C001")

            result.messageCount shouldBe 0
        }

        it("returns empty strings when client has null firstName / lastName") {
            every { clientRepository.findByClientNumber("C001") } returns client(firstName = null, lastName = null)
            every { personalMessageRepository.findAllByClientClientNumberOrderByIdDesc("C001") } returns emptyList()

            val result = layoutService.getLayoutData("C001")

            result.firstName shouldBe ""
            result.lastName shouldBe ""
        }

        it("throws NoSuchElementException when the client does not exist") {
            every { clientRepository.findByClientNumber("X") } returns null

            shouldThrow<NoSuchElementException> { layoutService.getLayoutData("X") }
        }
    }
})
