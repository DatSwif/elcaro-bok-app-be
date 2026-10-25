package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pl.elcaro.elcarobokapp.domain.Document
import pl.elcaro.elcarobokapp.domain.DocumentTranslation
import pl.elcaro.elcarobokapp.domain.DocumentTranslationId
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.DocumentRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider

class NewDocumentControllersItTest : BaseIntegrationTest({

    val self = this as NewDocumentControllersItTest

    beforeTest {
        self.clientRepository.deleteAll()
        self.documentRepository.deleteAll()
    }

    fun savedClientWithToken(languageCode: String = "pl"): Pair<Client, String> {
        val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = languageCode))
        val token = self.jwtTokenProvider.generateToken("C001")
        return client to token
    }

    fun seedDocuments() {
        val doc1 = Document("d1")
        doc1.translations.add(DocumentTranslation(DocumentTranslationId("pl", "d1"), doc1, "Regulamin świadczenia usług telekomunikacyjnych", "HTML content for d1"))
        self.documentRepository.save(doc1)

        val doc3 = Document("d3")
        doc3.translations.add(DocumentTranslation(DocumentTranslationId("pl", "d3"), doc3, "Regulamin promocji \"Oferta na bloki_lato 2024\"", "<h3>I. KORZYŚCI PROMOCJI</h3><ol><li>Obniżone opłaty...</li></ol>"))
        self.documentRepository.save(doc3)

        val p1 = Document("p1")
        p1.translations.add(DocumentTranslation(DocumentTranslationId("pl", "p1"), p1, "Polityka prywatności", "<h3>Polityka prywatności</h3><p>Twoje dane będą przetwarzane zgodnie z...</p>"))
        self.documentRepository.save(p1)

        val p2 = Document("p2")
        p2.translations.add(DocumentTranslation(DocumentTranslationId("pl", "p2"), p2, "Zgoda marketingowa", "Wyrażam zgodę na..."))
        self.documentRepository.save(p2)

        val m1 = Document("m1")
        m1.translations.add(DocumentTranslation(DocumentTranslationId("pl", "m1"), m1, "Zgody marketingowe", "Wyrażam zgodę na..."))
        self.documentRepository.save(m1)

        val t1 = Document("t1")
        t1.translations.add(DocumentTranslation(DocumentTranslationId("pl", "t1"), t1, "Regulamin rejestracji", "<h3>Regulamin</h3><p>Warunki korzystania z serwisu ebok...</p>"))
        self.documentRepository.save(t1)
    }

    describe("Public Documents") {
        it("GET /api/terms-of-service returns grouped documents") {
            seedDocuments()
            self.mockMvc.get("/api/terms-of-service") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].label") { exists() }
                jsonPath("$[0].documents[0].id") { value("d1") }
            }
        }

        it("GET /api/privacy-policy returns grouped documents") {
            seedDocuments()
            self.mockMvc.get("/api/privacy-policy") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].label") { exists() }
                jsonPath("$[0].documents[0].id") { value("p1") }
            }
        }
    }

    describe("Settings Documents (Protected)") {
        it("GET /api/settings/terms-of-service returns grouped documents") {
            seedDocuments()
            val (_, token) = savedClientWithToken()
            self.mockMvc.get("/api/settings/terms-of-service") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].label") { exists() }
            }
        }

        it("GET /api/settings/privacy-policy returns grouped documents") {
            seedDocuments()
            val (_, token) = savedClientWithToken()
            self.mockMvc.get("/api/settings/privacy-policy") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].label") { exists() }
            }
        }
    }

    describe("Auth Documents (Public)") {
        it("GET /api/auth/terms-of-service returns document content") {
            seedDocuments()
            self.mockMvc.get("/api/auth/terms-of-service") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("t1") }
                jsonPath("$.text") { exists() }
            }
        }

        it("GET /api/auth/privacy-policy returns document content") {
            seedDocuments()
            self.mockMvc.get("/api/auth/privacy-policy") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("p1") }
                jsonPath("$.text") { exists() }
            }
        }

        it("GET /api/auth/marketing-consents returns document content") {
            seedDocuments()
            self.mockMvc.get("/api/auth/marketing-consents") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("m1") }
                jsonPath("$.text") { exists() }
            }
        }
    }

    describe("Services Documents (Protected)") {
        it("GET /api/services/promotions-regulations returns document content") {
            seedDocuments()
            val (_, token) = savedClientWithToken()
            self.mockMvc.get("/api/services/promotions-regulations") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("d3") }
                jsonPath("$.text") { exists() }
            }
        }
    }

    describe("Document Content (Public)") {
        it("GET /api/documents/{id}/text returns raw text") {
            seedDocuments()
            self.mockMvc.get("/api/documents/d3/text") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("d3") }
                jsonPath("$.text") { exists() }
            }
        }

        it("GET /api/documents/{id}/pdf returns application/pdf") {
            seedDocuments()
            self.mockMvc.get("/api/documents/d3/pdf") {
                accept = MediaType.APPLICATION_PDF
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_PDF) }
                header { string("Content-Disposition", "attachment; filename=\"document-d3.pdf\"") }
            }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var documentRepository: DocumentRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
}
