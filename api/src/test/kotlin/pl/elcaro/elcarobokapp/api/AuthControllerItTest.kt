package pl.elcaro.elcarobokapp.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientEmail
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.domain.registration.ClientStatus
import pl.elcaro.elcarobokapp.infrastructure.ClientEmailRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.service.LoginRequest
import pl.elcaro.elcarobokapp.service.RegisterRequest
import pl.elcaro.elcarobokapp.service.VerifyCodeRequest
import tools.jackson.databind.ObjectMapper
import java.time.Instant

class AuthControllerItTest : BaseIntegrationTest({

    val self = this as AuthControllerItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    val phoneNumber = "+48123456789"
    val emailAddress = "client@example.com"
    val clientNumber = "C001"

    fun savedClientWithPhoneAndEmail(
        clientNumber: String = "C001",
        email: String = emailAddress,
        failedLoginCount: Int = 0,
        lockedUntil: Instant? = null,
        status: ClientStatus = ClientStatus.NEW,
    ): Client {
        val client = Client(
            clientNumber = clientNumber,
            languageCode = "pl",
            failedLoginCount = failedLoginCount,
            lockedUntil = lockedUntil,
            status = status,
        )
        client.phones.add(
            ClientPhone(
                client = client,
                phone = phoneNumber,
                expiresAt = Instant.now().plusSeconds(86400),
                createdAt = Instant.now(),
            )
        )
        client.emails.add(
            ClientEmail(
                client = client,
                email = email,
                createdAt = Instant.now(),
            )
        )
        return self.clientRepository.save(client)
    }

    fun savedClientWithPhone(
        clientNumber: String = "C001",
        failedLoginCount: Int = 0,
        lockedUntil: Instant? = null,
    ): Client {
        val client = Client(
            clientNumber = clientNumber,
            languageCode = "pl",
            failedLoginCount = failedLoginCount,
            lockedUntil = lockedUntil,
        )
        client.phones.add(
            ClientPhone(
                client = client,
                phone = phoneNumber,
                expiresAt = Instant.now().plusSeconds(86400),
                createdAt = Instant.now(),
            )
        )
        return self.clientRepository.save(client)
    }

    fun setPhoneCode(code: String = "1234", expiresAt: Instant = Instant.now().plusSeconds(300)) {
        val phone = self.clientPhoneRepository.findByPhone(phoneNumber)!!
        phone.code = code
        phone.expiresAt = expiresAt
        self.clientPhoneRepository.save(phone)
    }

    describe("POST /api/auth/login") {

        it("returns 200 when phone exists") {
            savedClientWithPhone()

            self.mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(LoginRequest(phoneNumber))
            }.andExpect {
                status { isOk() }
            }
        }

        it("returns 200 even when phone does not exist (enumeration protection)") {
            self.mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(LoginRequest("+48000000000"))
            }.andExpect {
                status { isOk() }
            }
        }

        it("returns 429 when account is locked") {
            savedClientWithPhone(lockedUntil = Instant.now().plusSeconds(300))

            val result = self.mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(LoginRequest(phoneNumber))
            }.andReturn()

            result.response.status shouldBe 429
        }
    }

    describe("POST /api/auth/verify-code") {

        it("returns 200 with a token on valid code") {
            savedClientWithPhone()
            setPhoneCode()

            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "1234"))
            }.andExpect {
                status { isOk() }
                jsonPath("$.token") { exists() }
                jsonPath("$.token") { isString() }
            }
        }

        it("returns 401 on wrong code") {
            savedClientWithPhone()
            setPhoneCode()

            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "0000"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when phone not found") {
            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest("+48000000000", "1234"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 on expired code") {
            savedClientWithPhone()
            setPhoneCode(expiresAt = Instant.now().minusSeconds(60))

            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "1234"))
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("increments failedLoginCount after a wrong code") {
            val client = savedClientWithPhone()
            setPhoneCode()

            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "0000"))
            }.andExpect {
                status { isUnauthorized() }
            }

            val updated = self.clientRepository.findById(client.id!!).get()
            updated.failedLoginCount shouldBe 1
        }

        it("returns 429 when account is locked") {
            savedClientWithPhone(lockedUntil = Instant.now().plusSeconds(300))

            val result = self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "1234"))
            }.andReturn()

            result.response.status shouldBe 429
        }

        it("locks the account after 5 failed verification attempts") {
            savedClientWithPhone()
            setPhoneCode()

            repeat(5) {
                self.mockMvc.post("/api/auth/verify-code") {
                    contentType = MediaType.APPLICATION_JSON
                    content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "0000"))
                }
            }

            val result = self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "0000"))
            }.andReturn()

            result.response.status shouldBe 429
            val locked = self.clientRepository.findByClientNumber("C001")!!
            locked.lockedUntil shouldNotBe null
        }

        it("resets failedLoginCount after a successful verification") {
            savedClientWithPhone(failedLoginCount = 3)
            setPhoneCode()

            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, "1234"))
            }.andExpect {
                status { isOk() }
            }

            val updated = self.clientRepository.findByClientNumber("C001")!!
            updated.failedLoginCount shouldBe 0
        }
    }

    describe("GET /api/auth/dev/code") {

        it("returns 404 when dev test login is disabled (default)") {
            savedClientWithPhone()
            setPhoneCode()

            self.mockMvc.get("/api/auth/dev/code") {
                param("phoneNumber", phoneNumber)
            }.andExpect {
                status { isNotFound() }
            }
        }

        it("returns 400 when phoneNumber is not in E.164 format") {
            self.mockMvc.get("/api/auth/dev/code") {
                param("phoneNumber", "not-a-phone")
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    describe("POST /api/auth/register") {

        it("returns 200 when clientNumber, email and phone all match") {
            savedClientWithPhoneAndEmail()

            self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest(clientNumber, emailAddress, phoneNumber))
            }.andExpect {
                status { isOk() }
            }

            val phone = self.clientPhoneRepository.findByPhone(phoneNumber)!!
            phone.code shouldNotBe null
        }

        it("returns 200 when clientNumber does not exist (enumeration protection)") {
            self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest("WRONG", emailAddress, phoneNumber))
            }.andExpect {
                status { isOk() }
            }
        }

        it("returns 200 when email does not match (enumeration protection)") {
            savedClientWithPhoneAndEmail()

            self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest(clientNumber, "wrong@example.com", phoneNumber))
            }.andExpect {
                status { isOk() }
            }
        }

        it("returns 200 when phone does not match (enumeration protection)") {
            savedClientWithPhoneAndEmail()

            self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest(clientNumber, emailAddress, "+48000000000"))
            }.andExpect {
                status { isOk() }
            }
        }

        it("returns 429 when account is locked") {
            savedClientWithPhoneAndEmail(lockedUntil = Instant.now().plusSeconds(300))

            val result = self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest(clientNumber, emailAddress, phoneNumber))
            }.andReturn()

            result.response.status shouldBe 429
        }

        it("returns 200 when client is already ACTIVE (enumeration protection)") {
            savedClientWithPhoneAndEmail(status = ClientStatus.ACTIVE)

            self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest(clientNumber, emailAddress, phoneNumber))
            }.andExpect {
                status { isOk() }
            }
        }

        it("full registration flow: register then verify-code returns JWT and activates client") {
            savedClientWithPhoneAndEmail()

            self.mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(RegisterRequest(clientNumber, emailAddress, phoneNumber))
            }.andExpect {
                status { isOk() }
            }

            val phone = self.clientPhoneRepository.findByPhone(phoneNumber)!!
            val code = phone.code!!

            self.mockMvc.post("/api/auth/verify-code") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber, code))
            }.andExpect {
                status { isOk() }
                jsonPath("$.token") { exists() }
                jsonPath("$.token") { isString() }
            }

            val updated = self.clientRepository.findByClientNumber(clientNumber)!!
            updated.status shouldBe ClientStatus.ACTIVE
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var clientPhoneRepository: ClientPhoneRepository

    @Autowired
    lateinit var clientEmailRepository: ClientEmailRepository

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository
}
