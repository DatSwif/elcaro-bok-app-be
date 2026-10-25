package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pl.elcaro.elcarobokapp.domain.Document
import pl.elcaro.elcarobokapp.domain.DocumentTranslation
import pl.elcaro.elcarobokapp.domain.DocumentTranslationId
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientServiceRepository
import pl.elcaro.elcarobokapp.infrastructure.DocumentRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.security.TestSecurityConfigurationFull.Companion.expiredToken
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import java.time.Instant

class SecurityEndpointProtectionItTest : BaseIntegrationTest({

    val self = this as SecurityEndpointProtectionItTest

    fun savedDocument(): Document {
        val doc = Document(id = "Doc1")
        doc.translations = mutableListOf(
            DocumentTranslation(
                id = DocumentTranslationId("en", "Doc1"),
                document = doc,
                name = "Document 1",
                text = "Public content",
            ),
        )
        return self.documentRepository.save(doc)
    }

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.clientRepository.deleteAll()
        self.documentRepository.deleteAll()
    }

    describe("protected endpoints - /api/clients") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when Authorization header has wrong format") {
            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Basic dXNlcjpwYXNz")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))

            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 200 when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/clients") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.clientNumber") { value("C001") }
            }
        }
    }

    describe("protected endpoints - /api/documents") {

        it("returns 401 when no Authorization header is provided") {
            savedDocument()

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            savedDocument()

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "en"))
            savedDocument()

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 200 when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "en"))
            savedDocument()
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("Doc1") }
            }
        }
    }

    describe("protected endpoints - /api/clients/lang-notification-settings") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.get("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 200 when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
            }
        }
    }

    describe("protected endpoints - /api/services") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/services") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.get("/api/services") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))

            self.mockMvc.get("/api/services") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 200 when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/services") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
            }
        }
    }

    describe("protected endpoints - /api/services/{id}") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/services/1") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.get("/api/services/1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))

            self.mockMvc.get("/api/services/1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 404 (not 401) when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/services/999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("protected endpoints - POST /api/services/{id}/packages/{packageId}/init and /verify") {

        val initEndpoint = "/api/services/1/packages/1/init"
        val verifyEndpoint = "/api/services/1/packages/1/verify"

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.post(initEndpoint) { accept = MediaType.APPLICATION_JSON }.andExpect { status { isUnauthorized() } }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.post(initEndpoint) {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect { status { isUnauthorized() } }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            self.mockMvc.post(initEndpoint) {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect { status { isUnauthorized() } }
        }

        it("returns 404 (not 401) for unknown service/package when a valid JWT token is provided") {
            val client = Client(clientNumber = "C001", languageCode = "pl")
            client.phones.add(
                ClientPhone(client = client, phone = "123456789", expiresAt = Instant.now().plusSeconds(86400), createdAt = Instant.now())
            )
            self.clientRepository.save(client)
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.post(initEndpoint) {
                param("phoneNumber", "123456789")
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }
            val code = self.clientPhoneRepository.findByPhone("123456789")!!.code!!

            self.mockMvc.post("/api/services/999/packages/999/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"phoneNumber": "123456789", "code": "$code"}"""
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }
    }

    describe("protected endpoints - DELETE /api/services/{id}/packages/{packageId}") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.delete("/api/services/1/packages/1") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.delete("/api/services/1/packages/1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))

            self.mockMvc.delete("/api/services/1/packages/1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 404 (not 401) when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.delete("/api/services/999/packages/999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("protected endpoints - /api/hardware") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))

            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 200 when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
            }
        }
    }

    describe("protected endpoints - /api/hardware/{id}") {

        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/hardware/999") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an invalid token is provided") {
            self.mockMvc.get("/api/hardware/999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer this.is.not.a.valid.token")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 401 when an expired token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))

            self.mockMvc.get("/api/hardware/999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${expiredToken()}")
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        it("returns 404 (not 401) when a valid JWT token is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware/999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("protected endpoints - /api/layout") {
        it("returns 401 when no Authorization header is provided") {
            self.mockMvc.get("/api/layout") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("protected endpoints - Support and FAQ") {
        val protectedEndpoints = listOf(
            "/api/contact-help",
            "/api/faq/categories",
            "/api/faq/categories/billing",
            "/api/faq/search?q=test"
        )

        protectedEndpoints.forEach { endpoint ->
            it("returns 401 when no Authorization header is provided for $endpoint") {
                self.mockMvc.get(endpoint) {
                    accept = MediaType.APPLICATION_JSON
                }.andExpect {
                    status { isUnauthorized() }
                }
            }
        }

        protectedEndpoints.forEach { endpoint ->
            it("returns 200 when a valid JWT token is provided for $endpoint") {
                self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
                val token = self.jwtTokenProvider.generateToken("C001")

                self.mockMvc.get(endpoint) {
                    accept = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                }
            }
        }
    }

    describe("public endpoints - /api/auth") {

        it("does not require authentication") {
            self.mockMvc.get("/api/auth/login") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isMethodNotAllowed() }
            }
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var clientPhoneRepository: ClientPhoneRepository

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository

    @Autowired
    lateinit var clientServiceRepository: ClientServiceRepository
}
