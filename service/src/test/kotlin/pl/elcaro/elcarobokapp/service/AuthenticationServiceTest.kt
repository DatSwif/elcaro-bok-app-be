package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.domain.registration.ClientStatus
import pl.elcaro.elcarobokapp.domain.registration.ConsentType
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ConsentTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import java.time.Instant
import java.util.*

class AuthenticationServiceTest : DescribeSpec({

    val clientRepository = mockk<ClientRepository>()
    val clientPhoneRepository = mockk<ClientPhoneRepository>()
    val consentTypeRepository = mockk<ConsentTypeRepository>()
    val userConsentRepository = mockk<UserConsentRepository>()
    val jwtTokenProvider = mockk<JwtTokenProvider>()
    val smsService = mockk<SmsService>(relaxed = true)
    val authenticationService = AuthenticationService(clientRepository, clientPhoneRepository, consentTypeRepository, userConsentRepository, jwtTokenProvider, smsService)

    beforeEach { clearAllMocks() }

    val phoneNumber = "+48123456789"
    val emailAddress = "client@example.com"
    val clientNumber = "C001"

    fun client(
        clientNumber: String = "C001",
        failedLoginCount: Int = 0,
        lockedUntil: Instant? = null,
    ) = Client(
        clientNumber = clientNumber,
        languageCode = "pl",
        failedLoginCount = failedLoginCount,
        lockedUntil = lockedUntil,
    ).also { it.id = 1L }

    fun clientPhone(
        client: Client,
        code: String? = null,
        expiresAt: Instant = Instant.now().plusSeconds(300),
    ) = ClientPhone(
        client = client,
        phone = phoneNumber,
        code = code,
        expiresAt = expiresAt,
        createdAt = Instant.now(),
    ).also { it.id = 1 }

    describe("requestCode") {

        it("sets code and expiresAt on ClientPhone and saves it") {
            val c = client()
            val cp = clientPhone(c)
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientPhoneRepository.save(cp) } returns cp

            authenticationService.requestCode(LoginRequest(phoneNumber))

            cp.code shouldNotBe null
            cp.expiresAt.isAfter(Instant.now()) shouldBe true
            verify(exactly = 1) { clientPhoneRepository.save(cp) }
        }

        it("silently returns when phone not found (enumeration protection)") {
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns null

            shouldNotThrowAny {
                authenticationService.requestCode(LoginRequest(phoneNumber))
            }
        }

        it("throws AccountLockedException when account is locked") {
            val c = client(lockedUntil = Instant.now().plusSeconds(60))
            val cp = clientPhone(c)
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp

            shouldThrow<AccountLockedException> {
                authenticationService.requestCode(LoginRequest(phoneNumber))
            }
        }

        it("overwrites any existing code on the phone") {
            val c = client()
            val cp = clientPhone(c, code = "old-code")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientPhoneRepository.save(cp) } returns cp

            authenticationService.requestCode(LoginRequest(phoneNumber))

            cp.code shouldNotBe "old-code"
            cp.code shouldNotBe null
        }
    }

    describe("verifyCode") {

        it("returns LoginResponse with token on valid code") {
            val c = client()
            val cp = clientPhone(c, code = "123456")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientPhoneRepository.save(cp) } returns cp
            every { clientRepository.save(c) } returns c
            every { jwtTokenProvider.generateToken("C001") } returns "test-jwt-token"

            val result = authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))

            result.token shouldBe "test-jwt-token"
            cp.code shouldBe null
            c.failedLoginCount shouldBe 0
            c.lockedUntil shouldBe null
        }

        it("throws AuthenticationException when phone not found") {
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns null

            shouldThrow<AuthenticationException> {
                authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))
            }
        }

        it("throws AuthenticationException when no code is set on the phone") {
            val c = client()
            val cp = clientPhone(c, code = null)
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientRepository.save(c) } returns c

            shouldThrow<AuthenticationException> {
                authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))
            }

            c.failedLoginCount shouldBe 1
        }

        it("throws AuthenticationException on wrong code and increments failedLoginCount") {
            val c = client(failedLoginCount = 0)
            val cp = clientPhone(c, code = "123456")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientRepository.save(c) } returns c

            shouldThrow<AuthenticationException> {
                authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "000000"))
            }

            c.failedLoginCount shouldBe 1
            verify(exactly = 1) { clientRepository.save(c) }
        }

        it("throws AuthenticationException on expired code") {
            val c = client()
            val cp = clientPhone(c, code = "123456", expiresAt = Instant.now().minusSeconds(60))
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientRepository.save(c) } returns c

            shouldThrow<AuthenticationException> {
                authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))
            }

            c.failedLoginCount shouldBe 1
        }

        it("locks the account after MAX_FAILED_ATTEMPTS wrong codes") {
            val c = client(failedLoginCount = 4)
            val cp = clientPhone(c, code = "123456")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientRepository.save(c) } returns c

            shouldThrow<AuthenticationException> {
                authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "000000"))
            }

            c.failedLoginCount shouldBe 0
            c.lockedUntil shouldNotBe null
        }

        it("throws AccountLockedException when account is locked") {
            val c = client(lockedUntil = Instant.now().plusSeconds(60))
            val cp = clientPhone(c, code = "123456")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp

            shouldThrow<AccountLockedException> {
                authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))
            }
        }

        it("resets failedLoginCount on successful verification") {
            val c = client(failedLoginCount = 3)
            val cp = clientPhone(c, code = "123456")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientPhoneRepository.save(cp) } returns cp
            every { clientRepository.save(c) } returns c
            every { jwtTokenProvider.generateToken("C001") } returns "test-jwt-token"

            authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))

            c.failedLoginCount shouldBe 0
            c.lockedUntil shouldBe null
        }

        it("clears the code after successful verification to prevent reuse") {
            val c = client()
            val cp = clientPhone(c, code = "123456")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp
            every { clientPhoneRepository.save(cp) } returns cp
            every { clientRepository.save(c) } returns c
            every { jwtTokenProvider.generateToken("C001") } returns "test-jwt-token"

            authenticationService.verifyCode(VerifyCodeRequest(phoneNumber, "123456"))

            cp.code shouldBe null
            verify(exactly = 1) { clientPhoneRepository.save(cp) }
        }
    }

    describe("findCodeForPhone") {

        it("returns null when devTestLoginEnabled is false") {
            val c = client()
            val cp = clientPhone(c, code = "9999")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp

            authenticationService.findCodeForPhone(phoneNumber) shouldBe null
        }

        it("returns the stored code when devTestLoginEnabled is true") {
            val service = AuthenticationService(
                clientRepository, clientPhoneRepository, consentTypeRepository,
                userConsentRepository, jwtTokenProvider, smsService, devTestLoginEnabled = true,
            )
            val c = client()
            val cp = clientPhone(c, code = "9999")
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp

            service.findCodeForPhone(phoneNumber) shouldBe "9999"
        }

        it("returns null when devTestLoginEnabled is true but no phone matches") {
            val service = AuthenticationService(
                clientRepository, clientPhoneRepository, consentTypeRepository,
                userConsentRepository, jwtTokenProvider, smsService, devTestLoginEnabled = true,
            )
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns null

            service.findCodeForPhone(phoneNumber) shouldBe null
        }

        it("returns null when devTestLoginEnabled is true but no code is set") {
            val service = AuthenticationService(
                clientRepository, clientPhoneRepository, consentTypeRepository,
                userConsentRepository, jwtTokenProvider, smsService, devTestLoginEnabled = true,
            )
            val c = client()
            val cp = clientPhone(c, code = null)
            every { clientPhoneRepository.findByPhone(phoneNumber) } returns cp

            service.findCodeForPhone(phoneNumber) shouldBe null
        }
    }

    describe("requestRegistrationCode") {

        val registerRequest = RegisterRequest(clientNumber, emailAddress, phoneNumber)

        fun clientWithPhone(): Pair<Client, ClientPhone> {
            val c = client()
            val cp = clientPhone(c)
            c.phones = mutableSetOf(cp)
            return c to cp
        }

        fun mockConsentSave() {
            val consentType = ConsentType().apply { id = 1 }
            every { consentTypeRepository.findByTranslationsCode("MARKETING") } returns consentType
            every { userConsentRepository.findById(any()) } returns Optional.empty()
            every { userConsentRepository.save(any()) } answers { firstArg() }
        }

        it("sets code and expiresAt on ClientPhone when all fields match") {
            val (c, cp) = clientWithPhone()
            every { clientRepository.findByClientNumberAndPhonesPhoneAndEmailsEmail(clientNumber, phoneNumber, emailAddress) } returns c
            every { clientPhoneRepository.save(cp) } returns cp
            mockConsentSave()

            authenticationService.requestRegistrationCode(registerRequest)

            cp.code shouldNotBe null
            cp.expiresAt.isAfter(Instant.now()) shouldBe true
            verify(exactly = 1) { clientPhoneRepository.save(cp) }
        }

        it("silently returns when no matching client/phone/email combination found (enumeration protection)") {
            every { clientRepository.findByClientNumberAndPhonesPhoneAndEmailsEmail(clientNumber, phoneNumber, emailAddress) } returns null

            shouldNotThrowAny {
                authenticationService.requestRegistrationCode(registerRequest)
            }
        }

        it("throws AccountLockedException when account is locked") {
            val c = client(lockedUntil = Instant.now().plusSeconds(60))
            c.phones = mutableSetOf(clientPhone(c))
            every { clientRepository.findByClientNumberAndPhonesPhoneAndEmailsEmail(clientNumber, phoneNumber, emailAddress) } returns c

            shouldThrow<AccountLockedException> {
                authenticationService.requestRegistrationCode(registerRequest)
            }
        }

        it("silently returns when client status is not NEW (enumeration protection)") {
            val c = client().also { it.status = ClientStatus.ACTIVE }
            c.phones = mutableSetOf(clientPhone(c))
            every { clientRepository.findByClientNumberAndPhonesPhoneAndEmailsEmail(clientNumber, phoneNumber, emailAddress) } returns c

            shouldNotThrowAny {
                authenticationService.requestRegistrationCode(registerRequest)
            }
        }
    }
})
