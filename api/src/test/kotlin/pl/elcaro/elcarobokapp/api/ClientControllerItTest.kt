package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientEmail
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.service.ClientUpdateRequest
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import tools.jackson.databind.ObjectMapper
import java.time.Instant

class ClientControllerItTest : BaseIntegrationTest({

    val self = this as ClientControllerItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    describe("GET /api/clients") {

        it("returns 200 with the authenticated client's data") {
            val client = Client(
                clientNumber = "C001",
                firstName = "Zbigniew",
                lastName = "Kuźniecowsk",
                organizationName = "adadada",
                languageCode = "pl",
                failedLoginCount = 0
            )
            client.emails.add(ClientEmail(client = client, email = "client1@example.cadada", createdAt = Instant.now()))
            client.emails.add(ClientEmail(client = client, email = "client2@example.cadada", createdAt = Instant.now()))
            client.phones.add(ClientPhone(client = client, phone = "+48123456789", expiresAt = Instant.now().plusSeconds(1000), createdAt = Instant.now()))
            client.phones.add(ClientPhone(client = client, phone = "+48888888888", expiresAt = Instant.now().plusSeconds(1000), createdAt = Instant.now()))
            self.clientRepository.save(client)
            
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.clientNumber") { value("C001") }
                jsonPath("$.firstName") { value("Zbigniew") }
                jsonPath("$.lastName") { value("Kuźniecowsk") }
                jsonPath("$.organizationName") { value("adadada") }
                jsonPath("$.languageCode") { value("pl") }
                jsonPath("$.emails") { isArray() }
                jsonPath("$.emails.length()") { value(2) }
                jsonPath("$.emails[0]") { value("client1@example.cadada") }
                jsonPath("$.emails[1]") { value("client2@example.cadada") }
                jsonPath("$.phones") { isArray() }
                jsonPath("$.phones.length()") { value(2) }
                jsonPath("$.phones[0]") { value("+48123456789") }
                jsonPath("$.phones[1]") { value("+48888888888") }
            }
        }

        it("returns 404 when authenticated client does not exist in database") {
            val token = self.jwtTokenProvider.generateToken("NONEXISTENT")

            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("PUT /api/clients") {

        it("returns 200 with the updated client and contact info") {
            val client = Client(
                clientNumber = "C001",
                firstName = "Jan",
                lastName = "Kowalski",
                languageCode = "pl"
            )
            client.emails.add(ClientEmail(client = client, email = "old@example.com", createdAt = Instant.now()))
            self.clientRepository.save(client)

            val token = self.jwtTokenProvider.generateToken("C001")
            val request = ClientUpdateRequest(
                firstName = "Piotr",
                emails = listOf("new1@example.com", "new2@example.com"),
                phones = listOf("+48987654321")
            )

            self.mockMvc.put("/api/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.firstName") { value("Piotr") }
                jsonPath("$.clientNumber") { value("C001") }
                jsonPath("$.emails.length()") { value(2) }
                jsonPath("$.emails[0]") { value("new1@example.com") }
                jsonPath("$.phones.length()") { value(1) }
                jsonPath("$.phones[0]") { value("+48987654321") }
            }
        }

        it("returns 404 when authenticated client does not exist in database") {
            val token = self.jwtTokenProvider.generateToken("NONEXISTENT")
            val request = ClientUpdateRequest(firstName = "X")

            self.mockMvc.put("/api/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }

        it("returns 409 when adding a phone already owned by another client") {
            val owner = Client(clientNumber = "C001", languageCode = "pl")
            owner.phones.add(
                ClientPhone(client = owner, phone = "+48111222333", expiresAt = Instant.now().plusSeconds(1000), createdAt = Instant.now())
            )
            self.clientRepository.save(owner)
            val other = Client(clientNumber = "C002", languageCode = "pl")
            self.clientRepository.save(other)

            val token = self.jwtTokenProvider.generateToken("C002")
            val request = ClientUpdateRequest(phones = listOf("+48111222333"))

            self.mockMvc.put("/api/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isConflict() }
                jsonPath("$.status") { value(409) }
                jsonPath("$.message") { value("Phone number is already in use") }
            }
        }

        it("returns 409 when adding an email already owned by another client") {
            val owner = Client(clientNumber = "C001", languageCode = "pl")
            owner.emails.add(ClientEmail(client = owner, email = "taken@example.com", createdAt = Instant.now()))
            self.clientRepository.save(owner)
            val other = Client(clientNumber = "C002", languageCode = "pl")
            self.clientRepository.save(other)

            val token = self.jwtTokenProvider.generateToken("C002")
            val request = ClientUpdateRequest(emails = listOf("taken@example.com"))

            self.mockMvc.put("/api/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(request)
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isConflict() }
                jsonPath("$.status") { value(409) }
                jsonPath("$.message") { value("Email address is already in use") }
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
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository
}
