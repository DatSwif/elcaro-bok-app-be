package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import pl.elcaro.elcarobokapp.domain.messages.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientGlobalMessageStateRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.GlobalMessageRepository
import pl.elcaro.elcarobokapp.infrastructure.PersonalMessageRepository
import java.time.Instant
import java.util.*

class MessagesServiceTest : DescribeSpec({

    val globalMessageRepository = mockk<GlobalMessageRepository>()
    val clientGlobalMessageStateRepository = mockk<ClientGlobalMessageStateRepository>()
    val personalMessageRepository = mockk<PersonalMessageRepository>()
    val clientRepository = mockk<ClientRepository>()
    val service = MessagesService(
        globalMessageRepository,
        clientGlobalMessageStateRepository,
        personalMessageRepository,
        clientRepository,
    )

    beforeEach { clearAllMocks() }

    fun client() = Client(clientNumber = "C001", languageCode = "pl").also { it.id = 1L }

    fun translation(lang: String, title: String, content: String, messageId: Long) =
        GlobalMessageTranslation(
            id = GlobalMessageTranslationId(languageCode = lang, globalMessageId = messageId),
            title = title,
            content = content,
        )

    fun globalMessage(id: Long, vararg translations: GlobalMessageTranslation) =
        GlobalMessage().apply {
            this.id = id
            this.translations = translations.toMutableSet()
        }

    fun state(message: GlobalMessage, isRead: Boolean = false, isDeleted: Boolean = false) =
        ClientGlobalMessageState(
            client = client(),
            globalMessage = message,
            isRead = isRead,
            isDeleted = isDeleted,
        )

    fun personalMessage(id: Long, isRead: Boolean = false) = PersonalMessage(
        title = "personal $id",
        level = 1,
        isRead = isRead,
        client = client(),
    ).apply { this.id = id }

    describe("listGlobal") {

        it("filters out messages marked as deleted in state") {
            val msg1 = globalMessage(10, translation("pl", "Hello", "body", 10))
            val msg2 = globalMessage(11, translation("pl", "Bye", "body", 11))
            every { globalMessageRepository.findAllByOrderByIdDesc() } returns listOf(msg1, msg2)
            every { clientGlobalMessageStateRepository.findAllByClientClientNumber("C001") } returns
                    listOf(state(msg2, isDeleted = true))

            val result = service.listGlobal("C001", Locale("pl"))

            result shouldHaveSize 1
            result[0].id shouldBe 10
        }

        it("defaults isRead to false when no state row exists") {
            val msg = globalMessage(5, translation("pl", "Hello", "body", 5))
            every { globalMessageRepository.findAllByOrderByIdDesc() } returns listOf(msg)
            every { clientGlobalMessageStateRepository.findAllByClientClientNumber("C001") } returns emptyList()

            val result = service.listGlobal("C001", Locale("pl"))

            result[0].isRead shouldBe false
            result[0].readAt shouldBe null
        }

        it("falls back to first translation when no pl/locale match exists") {
            val msg = globalMessage(7, translation("fr", "Bonjour", "corps", 7))
            every { globalMessageRepository.findAllByOrderByIdDesc() } returns listOf(msg)
            every { clientGlobalMessageStateRepository.findAllByClientClientNumber("C001") } returns emptyList()

            val result = service.listGlobal("C001", Locale("pl"))

            result[0].title shouldBe "Bonjour"
        }

        it("uses default 'No title' when message has no translations at all") {
            val msg = globalMessage(8)
            every { globalMessageRepository.findAllByOrderByIdDesc() } returns listOf(msg)
            every { clientGlobalMessageStateRepository.findAllByClientClientNumber("C001") } returns emptyList()

            val result = service.listGlobal("C001", Locale("pl"))

            result[0].title shouldBe "No title"
            result[0].content shouldBe "No content"
        }
    }

    describe("markGlobalRead") {

        it("creates a new state row marked as read when none exists") {
            val msg = globalMessage(10)
            every {
                clientGlobalMessageStateRepository.findByClientClientNumberAndGlobalMessageId("C001", 10)
            } returns null
            every { globalMessageRepository.findById(10) } returns Optional.of(msg)
            every { clientRepository.findByClientNumber("C001") } returns client()
            val saved = slot<ClientGlobalMessageState>()
            every { clientGlobalMessageStateRepository.save(capture(saved)) } answers { saved.captured }

            service.markGlobalRead("C001", 10)

            saved.captured.isRead shouldBe true
            saved.captured.readAt shouldNotBe null
        }

        it("is idempotent when state is already marked as read") {
            val msg = globalMessage(10)
            val existing = state(msg, isRead = true).apply { readAt = Instant.parse("2024-01-01T00:00:00Z") }
            every {
                clientGlobalMessageStateRepository.findByClientClientNumberAndGlobalMessageId("C001", 10)
            } returns existing
            every { clientGlobalMessageStateRepository.save(existing) } returns existing

            service.markGlobalRead("C001", 10)

            existing.isRead shouldBe true
            existing.readAt shouldBe Instant.parse("2024-01-01T00:00:00Z")
            verify(exactly = 1) { clientGlobalMessageStateRepository.save(existing) }
        }
    }

    describe("deleteGlobal") {
        it("sets isDeleted=true and saves") {
            val msg = globalMessage(10)
            val existing = state(msg)
            every {
                clientGlobalMessageStateRepository.findByClientClientNumberAndGlobalMessageId("C001", 10)
            } returns existing
            every { clientGlobalMessageStateRepository.save(existing) } returns existing

            service.deleteGlobal("C001", 10)

            existing.isDeleted shouldBe true
            verify(exactly = 1) { clientGlobalMessageStateRepository.save(existing) }
        }
    }

    describe("listPersonal") {
        it("maps personal messages to responses") {
            every { personalMessageRepository.findAllByClientClientNumberOrderByIdDesc("C001") } returns
                    listOf(personalMessage(1, isRead = true), personalMessage(2, isRead = false))

            val result = service.listPersonal("C001", Locale("pl"))

            result shouldHaveSize 2
            result[0].id shouldBe 1
            result[0].isRead shouldBe true
            result[1].isRead shouldBe false
        }

        it("returns empty list when client has no personal messages") {
            every { personalMessageRepository.findAllByClientClientNumberOrderByIdDesc("C001") } returns emptyList()

            service.listPersonal("C001", Locale("pl")).shouldBeEmpty()
        }
    }

    describe("markPersonalRead") {
        it("throws when message not found") {
            every { personalMessageRepository.findByIdAndClientClientNumber(99, "C001") } returns null

            shouldThrow<NoSuchElementException> { service.markPersonalRead("C001", 99) }
        }

        it("is idempotent and does not save when already read") {
            val msg = personalMessage(1, isRead = true)
            every { personalMessageRepository.findByIdAndClientClientNumber(1, "C001") } returns msg

            service.markPersonalRead("C001", 1)

            verify(exactly = 0) { personalMessageRepository.save(any()) }
        }

        it("marks unread message as read and saves") {
            val msg = personalMessage(1, isRead = false)
            every { personalMessageRepository.findByIdAndClientClientNumber(1, "C001") } returns msg
            every { personalMessageRepository.save(msg) } returns msg

            service.markPersonalRead("C001", 1)

            msg.isRead shouldBe true
            msg.readAt shouldNotBe null
            verify(exactly = 1) { personalMessageRepository.save(msg) }
        }
    }

    describe("deletePersonal") {
        it("throws when message not found") {
            every { personalMessageRepository.findByIdAndClientClientNumber(99, "C001") } returns null

            shouldThrow<NoSuchElementException> { service.deletePersonal("C001", 99) }
        }

        it("delegates to repository.delete when found") {
            val msg = personalMessage(1)
            every { personalMessageRepository.findByIdAndClientClientNumber(1, "C001") } returns msg
            every { personalMessageRepository.delete(msg) } returns Unit

            service.deletePersonal("C001", 1)

            verify(exactly = 1) { personalMessageRepository.delete(msg) }
        }
    }
})
