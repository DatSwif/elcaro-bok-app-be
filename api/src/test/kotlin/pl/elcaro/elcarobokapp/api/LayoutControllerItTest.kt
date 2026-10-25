package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pl.elcaro.elcarobokapp.domain.messages.PersonalMessage
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.PersonalMessageRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider

class LayoutControllerItTest : BaseIntegrationTest({

    val self = this as LayoutControllerItTest

    beforeTest {
        self.personalMessageRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    fun savedClientWithToken(clientNumber: String = "C001"): Pair<Client, String> {
        val client = self.clientRepository.save(
            Client(
                clientNumber = clientNumber,
                firstName = "Zbigniew",
                lastName = "Kuźniecowski",
                languageCode = "pl"
            )
        )
        val token = self.jwtTokenProvider.generateToken(clientNumber)
        return client to token
    }

    describe("GET /api/layout") {

        it("returns 200 OK with correct layout data for authenticated user") {
            val (client, token) = savedClientWithToken()
            
            // Add a personal message to ensure the count is correct
            self.personalMessageRepository.save(
                PersonalMessage(
                    title = "Test Message",
                    client = client,
                    level = 1
                )
            )

            self.mockMvc.get("/api/layout") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.firstName") { value("Zbigniew") }
                jsonPath("$.lastName") { value("Kuźniecowski") }
                jsonPath("$.messageCount") { value(1) }
            }
        }

        it("returns 401 when no token is provided") {
            self.mockMvc.get("/api/layout") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var personalMessageRepository: PersonalMessageRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider
}
