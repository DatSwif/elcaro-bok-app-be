package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.UserConsent
import pl.elcaro.elcarobokapp.domain.registration.UserConsentId
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientServiceRepository
import pl.elcaro.elcarobokapp.infrastructure.ConsentTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.service.ConsentUpdateRequest
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import tools.jackson.databind.ObjectMapper
import java.time.Instant

class ConsentControllerItTest : BaseIntegrationTest({

    val self = this as ConsentControllerItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.consentTypeRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    fun savedClientWithToken(languageCode: String = "en"): Pair<Client, String> {
        val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = languageCode))
        val token = self.jwtTokenProvider.generateToken("C001")
        return client to token
    }

    describe("GET /api/clients/consents") {

        it("returns 200 with all consent types and isGranted=false when no consents exist") {
            val (_, token) = savedClientWithToken()
            self.consentTypeRepository.save(testConsentTypeWithTranslations())

            self.mockMvc.get("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].code") { value("MARKETING") }
                jsonPath("$[0].title") { value("Marketing consent") }
                jsonPath("$[0].isGranted") { value(false) }
                jsonPath("$[0].grantedAt") { doesNotExist() }
            }
        }

        it("returns isGranted=true when consent exists for client") {
            val (client, token) = savedClientWithToken()
            val type = self.consentTypeRepository.save(testConsentTypeWithTranslations())
            self.userConsentRepository.save(
                UserConsent(
                    id = UserConsentId(clientId = client.id, consentTypeId = type.id),
                    client = client,
                    consentType = type,
                    isGranted = true,
                    grantedAt = Instant.now(),
                )
            )

            self.mockMvc.get("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].isGranted") { value(true) }
                jsonPath("$[0].grantedAt") { isNotEmpty() }
            }
        }

        it("returns title and description in Polish when client language is pl") {
            val (_, token) = savedClientWithToken(languageCode = "pl")
            self.consentTypeRepository.save(testConsentTypeWithTranslations())

            self.mockMvc.get("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].title") { value("Zgoda marketingowa") }
                jsonPath("$[0].description") { value("Zgoda na komunikację marketingową") }
            }
        }

        it("returns empty list when no consent types exist") {
            val (_, token) = savedClientWithToken()

            self.mockMvc.get("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isEmpty() }
            }
        }

        it("returns 401 when no auth token") {
            self.mockMvc.get("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("PUT /api/clients/consents") {

        it("creates consent and returns isGranted=true with title") {
            val (_, token) = savedClientWithToken()
            self.consentTypeRepository.save(testConsentTypeWithTranslations())
            val body = self.objectMapper.writeValueAsString(
                listOf(ConsentUpdateRequest(code = "MARKETING", isGranted = true))
            )

            self.mockMvc.put("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = body
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].title") { value("Marketing consent") }
                jsonPath("$[0].isGranted") { value(true) }
                jsonPath("$[0].grantedAt") { isNotEmpty() }
            }
        }

        it("updates existing consent to isGranted=false") {
            val (client, token) = savedClientWithToken()
            val type = self.consentTypeRepository.save(testConsentTypeWithTranslations())
            self.userConsentRepository.save(
                UserConsent(
                    id = UserConsentId(clientId = client.id, consentTypeId = type.id),
                    client = client,
                    consentType = type,
                    isGranted = true,
                    grantedAt = Instant.now(),
                )
            )
            val body = self.objectMapper.writeValueAsString(
                listOf(ConsentUpdateRequest(code = "MARKETING", isGranted = false))
            )

            self.mockMvc.put("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = body
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].isGranted") { value(false) }
            }
        }

        it("returns 400 for unknown consent code") {
            val (_, token) = savedClientWithToken()
            val body = self.objectMapper.writeValueAsString(
                listOf(ConsentUpdateRequest(code = "UNKNOWN_CODE", isGranted = true))
            )

            self.mockMvc.put("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = body
            }.andExpect {
                status { isBadRequest() }
            }
        }

        it("returns 401 when no auth token") {
            val body = self.objectMapper.writeValueAsString(
                listOf(ConsentUpdateRequest(code = "MARKETING", isGranted = true))
            )

            self.mockMvc.put("/api/clients/consents") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isUnauthorized() }
            }
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
    lateinit var consentTypeRepository: ConsentTypeRepository

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var clientServiceRepository: ClientServiceRepository
}
