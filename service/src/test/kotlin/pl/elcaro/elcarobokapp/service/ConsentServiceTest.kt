package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.elcaro.elcarobokapp.domain.registration.*
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ConsentTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import java.time.Instant
import java.util.*

class ConsentServiceTest : DescribeSpec({

    val clientRepository = mockk<ClientRepository>()
    val consentTypeRepository = mockk<ConsentTypeRepository>()
    val userConsentRepository = mockk<UserConsentRepository>()
    val service = ConsentService(clientRepository, consentTypeRepository, userConsentRepository)

    beforeEach { clearAllMocks() }

    val clientNumber = "C001"

    fun client() = Client(clientNumber = clientNumber, languageCode = "en").also { it.id = 1L }

    fun consentType(id: Int, enCode: String, plCode: String, enDesc: String, plDesc: String): ConsentType {
        val type = ConsentType()
        type.id = id
        type.translations = mutableListOf(
            ConsentTypeTranslation(
                id = ConsentTypeTranslationId("en", id),
                consentType = type,
                code = enCode,
                description = enDesc,
            ),
            ConsentTypeTranslation(
                id = ConsentTypeTranslationId("pl", id),
                consentType = type,
                code = plCode,
                description = plDesc,
            ),
        )
        return type
    }

    fun consent(client: Client, type: ConsentType, granted: Boolean): UserConsent =
        UserConsent(
            id = UserConsentId(clientId = client.id, consentTypeId = type.id),
            client = client,
            consentType = type,
            isGranted = granted,
            grantedAt = Instant.now(),
        )

    describe("getClientConsents") {

        it("returns all consent types with isGranted=false when no consents exist") {
            val c = client()
            val type = consentType(1, "MARKETING", "MARKETING", "Marketing consent", "Zgoda marketingowa")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findAll() } returns listOf(type)
            every { userConsentRepository.findByClient(c) } returns emptyList()

            val result = service.getClientConsents(clientNumber, Locale.ENGLISH)

            result.size shouldBe 1
            result[0].code shouldBe "MARKETING"
            result[0].isGranted shouldBe false
            result[0].grantedAt shouldBe null
        }

        it("returns isGranted=true when consent exists") {
            val c = client()
            val type = consentType(1, "MARKETING", "MARKETING", "Marketing consent", "Zgoda marketingowa")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findAll() } returns listOf(type)
            every { userConsentRepository.findByClient(c) } returns listOf(consent(c, type, granted = true))

            val result = service.getClientConsents(clientNumber, Locale.ENGLISH)

            result[0].isGranted shouldBe true
            result[0].grantedAt shouldNotBe null
        }

        it("uses locale to return translated description") {
            val c = client()
            val type = consentType(1, "MARKETING", "MARKETING", "Marketing consent", "Zgoda marketingowa")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findAll() } returns listOf(type)
            every { userConsentRepository.findByClient(c) } returns emptyList()

            val result = service.getClientConsents(clientNumber, Locale.forLanguageTag("pl"))

            result[0].description shouldBe "Zgoda marketingowa"
        }

        it("falls back to Polish when locale translation is not found") {
            val c = client()
            val type = consentType(1, "MARKETING", "MARKETING", "Marketing consent", "Zgoda marketingowa")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findAll() } returns listOf(type)
            every { userConsentRepository.findByClient(c) } returns emptyList()

            val result = service.getClientConsents(clientNumber, Locale.FRENCH)

            result[0].description shouldBe "Zgoda marketingowa"
        }

        it("throws NoSuchElementException when client not found") {
            every { clientRepository.findByClientNumber(clientNumber) } returns null

            shouldThrow<NoSuchElementException> {
                service.getClientConsents(clientNumber, Locale.ENGLISH)
            }
        }
    }

    describe("updateConsent") {

        it("creates new UserConsent when none exists") {
            val c = client()
            val type = consentType(1, "MARKETING", "MARKETING", "Marketing consent", "Zgoda marketingowa")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findByTranslationsCode("MARKETING") } returns type
            every { userConsentRepository.findById(any()) } returns Optional.empty()
            every { userConsentRepository.save(any()) } answers { firstArg() }

            service.updateConsent(clientNumber, ConsentUpdateRequest(code = "MARKETING", isGranted = true))

            verify(exactly = 1) { userConsentRepository.save(any()) }
        }

        it("updates existing UserConsent record") {
            val c = client()
            val type = consentType(1, "MARKETING", "MARKETING", "Marketing consent", "Zgoda marketingowa")
            val existing = consent(c, type, granted = true)
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findByTranslationsCode("MARKETING") } returns type
            every { userConsentRepository.findById(any()) } returns Optional.of(existing)
            every { userConsentRepository.save(any()) } answers { firstArg() }

            service.updateConsent(clientNumber, ConsentUpdateRequest(code = "MARKETING", isGranted = false))

            existing.isGranted shouldBe false
            verify(exactly = 1) { userConsentRepository.save(existing) }
        }

        it("throws IllegalArgumentException for unknown code") {
            val c = client()
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { consentTypeRepository.findByTranslationsCode("UNKNOWN") } returns null

            shouldThrow<IllegalArgumentException> {
                service.updateConsent(clientNumber, ConsentUpdateRequest(code = "UNKNOWN", isGranted = true))
            }
        }

        it("throws NoSuchElementException when client not found") {
            every { clientRepository.findByClientNumber(clientNumber) } returns null

            shouldThrow<NoSuchElementException> {
                service.updateConsent(clientNumber, ConsentUpdateRequest(code = "MARKETING", isGranted = true))
            }
        }
    }
})
