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

class DocumentControllerAuthLocaleItTest : BaseIntegrationTest({

    val self = this as DocumentControllerAuthLocaleItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.clientRepository.deleteAll()
        self.documentRepository.deleteAll()
    }

    describe("GET /api/documents/{doc_id} - authenticated locale resolution") {

        it("uses Accept-Language header when provided, ignoring client's stored language") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            self.documentRepository.save(testDocumentWithTranslations())
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                header("Accept-Language", "en")
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Document 1") }
                jsonPath("$.text") { value("English content") }
            }
        }

        it("falls back to client's stored language when no Accept-Language header is provided") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            self.documentRepository.save(testDocumentWithTranslations())
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Dokument 1") }
                jsonPath("$.text") { value("Treść po polsku") }
            }
        }

        it("falls back to Polish when authenticated client's language has no translation and no header") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "fr"))
            self.documentRepository.save(testDocumentWithTranslations())
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/documents/Doc1") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Dokument 1") }
                jsonPath("$.text") { value("Treść po polsku") }
            }
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository
}
