package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientEmail
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.infrastructure.ClientEmailRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import java.time.Instant

class ClientMapperTest : DescribeSpec({

    val clientPhoneRepository = mockk<ClientPhoneRepository>()
    val clientEmailRepository = mockk<ClientEmailRepository>()
    val mapper = ClientMapper(clientPhoneRepository, clientEmailRepository)

    beforeEach { clearAllMocks() }

    fun client(
        id: Long? = 1L,
        firstName: String? = "Jan",
        lastName: String? = "Kowalski",
        languageCode: String = "pl",
        emails: Set<String> = emptySet(),
        phones: Set<String> = emptySet(),
    ) = Client(
        clientNumber = "C001",
        firstName = firstName,
        lastName = lastName,
        languageCode = languageCode,
    ).also { c ->
        c.id = id
        c.emails = emails.map { ClientEmail(client = c, email = it, createdAt = Instant.now()) }.toMutableSet()
        c.phones = phones.map {
            ClientPhone(client = c, phone = it, expiresAt = Instant.now().plusSeconds(60), createdAt = Instant.now())
        }.toMutableSet()
    }

    describe("applyUpdate — simple fields") {

        it("updates firstName, lastName, organizationName, languageCode when present") {
            val c = client()
            val req = ClientUpdateRequest(
                firstName = "Anna",
                lastName = "Nowak",
                organizationName = "Elcaro",
                languageCode = "en",
            )

            mapper.applyUpdate(req, c)

            c.firstName shouldBe "Anna"
            c.lastName shouldBe "Nowak"
            c.organizationName shouldBe "Elcaro"
            c.languageCode shouldBe "en"
        }

        it("leaves fields untouched when not present in the request") {
            val c = client(firstName = "Jan")
            mapper.applyUpdate(ClientUpdateRequest(), c)
            c.firstName shouldBe "Jan"
        }
    }

    describe("applyUpdate — emails") {

        it("adds new emails after checking for duplicates") {
            val c = client(emails = setOf("old@x.pl"))
            every { clientEmailRepository.existsByEmailAndClientIdNot("new@x.pl", 1L) } returns false

            mapper.applyUpdate(ClientUpdateRequest(emails = listOf("old@x.pl", "new@x.pl")), c)

            c.emails.map { it.email } shouldContainExactlyInAnyOrder listOf("old@x.pl", "new@x.pl")
        }

        it("throws DuplicateContactException when an email is already used by another client") {
            val c = client(emails = setOf("old@x.pl"))
            every { clientEmailRepository.existsByEmailAndClientIdNot("dup@x.pl", 1L) } returns true

            val ex = shouldThrow<DuplicateContactException> {
                mapper.applyUpdate(ClientUpdateRequest(emails = listOf("old@x.pl", "dup@x.pl")), c)
            }
            ex.field shouldBe ContactField.EMAIL
        }

        it("removes emails missing from the request") {
            val c = client(emails = setOf("a@x.pl", "b@x.pl"))

            mapper.applyUpdate(ClientUpdateRequest(emails = listOf("a@x.pl")), c)

            c.emails.map { it.email } shouldContainExactlyInAnyOrder listOf("a@x.pl")
        }

        it("skips duplicate check when clientId is null") {
            val c = client(id = null)

            mapper.applyUpdate(ClientUpdateRequest(emails = listOf("new@x.pl")), c)

            c.emails.map { it.email } shouldContainExactlyInAnyOrder listOf("new@x.pl")
        }
    }

    describe("applyUpdate — phones") {

        it("adds new phones after checking for duplicates") {
            val c = client(phones = setOf("+48111"))
            every { clientPhoneRepository.existsByPhoneAndClientIdNot("+48222", 1L) } returns false

            mapper.applyUpdate(ClientUpdateRequest(phones = listOf("+48111", "+48222")), c)

            c.phones.map { it.phone } shouldContainExactlyInAnyOrder listOf("+48111", "+48222")
        }

        it("throws DuplicateContactException when a phone is already used by another client") {
            val c = client()
            every { clientPhoneRepository.existsByPhoneAndClientIdNot("+48999", 1L) } returns true

            val ex = shouldThrow<DuplicateContactException> {
                mapper.applyUpdate(ClientUpdateRequest(phones = listOf("+48999")), c)
            }
            ex.field shouldBe ContactField.PHONE
        }

        it("removes phones missing from the request") {
            val c = client(phones = setOf("+48111", "+48222"))

            mapper.applyUpdate(ClientUpdateRequest(phones = listOf("+48111")), c)

            c.phones.map { it.phone } shouldContainExactlyInAnyOrder listOf("+48111")
        }
    }

    describe("Client.toResponse") {
        it("maps emails and phones preserving insertion order") {
            val c = client(emails = setOf("b@x.pl", "a@x.pl"), phones = setOf("+48222", "+48111"))

            val response = c.toResponse()

            response.id shouldBe 1L
            response.clientNumber shouldBe "C001"
            response.emails shouldBe listOf("b@x.pl", "a@x.pl")
            response.phones shouldBe listOf("+48222", "+48111")
        }
    }
})
