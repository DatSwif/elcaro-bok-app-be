package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.DocumentRepository
import pl.elcaro.elcarobokapp.infrastructure.UserConsentRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider

class DocumentControllerItTest : BaseIntegrationTest({

    val self = this as DocumentControllerItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.clientRepository.deleteAll()
        self.documentRepository.deleteAll()
    }

    fun tokenForClient(languageCode: String = "en"): String {
        self.clientRepository.save(Client(clientNumber = "C001", languageCode = languageCode))
        return self.jwtTokenProvider.generateToken("C001")
    }

    describe("GET /api/documents") {

        it("returns 200 with id and name only (no text) in English by default") {
            val token = tokenForClient()
            self.documentRepository.save(testDocumentWithTranslations())

            self.mockMvc.get("/api/documents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value("Doc1") }
                jsonPath("$[0].name") { value("Document 1") }
                jsonPath("$[0].text") { doesNotExist() }
            }
        }

        it("returns Polish names when client language is pl") {
            val token = tokenForClient("pl")
            self.documentRepository.save(testDocumentWithTranslations())

            self.mockMvc.get("/api/documents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value("Doc1") }
                jsonPath("$[0].name") { value("Dokument 1") }
                jsonPath("$[0].text") { doesNotExist() }
            }
        }

        it("returns empty array when no documents exist") {
            val token = tokenForClient()

            self.mockMvc.get("/api/documents") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isEmpty() }
            }
        }
    }

    describe("GET /api/documents/{doc_id}") {

        it("returns 200 with English content by default") {
            val token = tokenForClient()
            self.documentRepository.save(testDocumentWithTranslations())

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("Doc1") }
                jsonPath("$.name") { value("Document 1") }
                jsonPath("$.text") { value("English content") }
            }
        }

        it("returns 200 with Polish content when client language is pl") {
            val token = tokenForClient("pl")
            self.documentRepository.save(testDocumentWithTranslations())

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("Doc1") }
                jsonPath("$.name") { value("Dokument 1") }
                jsonPath("$.text") { value("Treść po polsku") }
            }
        }

        it("returns German content when client language is de") {
            val token = tokenForClient("de")
            self.documentRepository.save(testDocumentWithTranslations())

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Dokument 1 DE") }
                jsonPath("$.text") { value("Deutscher Inhalt") }
            }
        }

        it("falls back to Polish for unsupported locale") {
            val token = tokenForClient("fr")
            self.documentRepository.save(testDocumentWithTranslations())

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Dokument 1") }
                jsonPath("$.text") { value("Treść po polsku") }
            }
        }

        it("returns 404 when document is not found") {
            val token = tokenForClient()

            self.mockMvc.get("/api/documents/NonExistent") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository
}
