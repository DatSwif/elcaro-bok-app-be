package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ConsentTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.service.ConsentUpdateRequest
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import tools.jackson.databind.ObjectMapper

class SettingsControllerItTest : BaseIntegrationTest({

    val self = this as SettingsControllerItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.consentTypeRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.clientAddressRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    describe("GET /api/settings/consents") {
        it("returns 200 and a list of consents for the authenticated user") {
            val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            self.consentTypeRepository.save(testConsentTypeWithTranslations())
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/settings/consents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$[0].code") { value("MARKETING") }
                jsonPath("$[0].title") { value("Zgoda marketingowa") }
                jsonPath("$[0].isGranted") { value(false) }
                jsonPath("$[0].isEditable") { value(true) }
            }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/settings/consents") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("PUT /api/settings/consents") {
        it("returns 200 and updates the consent status") {
            val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            self.consentTypeRepository.save(testConsentTypeWithTranslations())
            val token = self.jwtTokenProvider.generateToken("C001")

            val request = ConsentUpdateRequest(code = "MARKETING", isGranted = true)

            self.mockMvc.put("/api/settings/consents") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
            }

            val savedConsents = self.userConsentRepository.findByClient(client)
            assert(savedConsents.isNotEmpty())
            assert(savedConsents.first().isGranted)
        }

        it("returns 401 when trying to update without a token") {
            val request = ConsentUpdateRequest(code = "MARKETING", isGranted = true)

            self.mockMvc.put("/api/settings/consents") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var consentTypeRepository: ConsentTypeRepository
    @Autowired lateinit var userConsentRepository: UserConsentRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
    @Autowired lateinit var clientServiceRepository: pl.elcaro.elcarobokapp.infrastructure.ClientServiceRepository
    @Autowired lateinit var clientAddressRepository: pl.elcaro.elcarobokapp.infrastructure.ClientAddressRepository
}
