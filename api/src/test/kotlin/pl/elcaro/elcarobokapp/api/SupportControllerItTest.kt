package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider

class SupportControllerItTest : BaseIntegrationTest({

    val self = this as SupportControllerItTest

    beforeTest {
        self.clientRepository.deleteAll()
    }

    fun savedClient(languageCode: String = "pl"): Pair<Client, String> {
        val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = languageCode))
        val token = self.jwtTokenProvider.generateToken("C001")
        return client to token
    }

    describe("GET /api/contact-help") {
        it("returns 200 OK and contact help data in Polish") {
            val (_, token) = savedClient("pl")
            self.mockMvc.get("/api/contact-help") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.phone.number") { value("409 901 990") }
                jsonPath("$.phone.schedule.days") { value("Pn-Nd") }
                jsonPath("$.email.address") { value("info@elcaroco.pl") }
            }
        }

        it("returns 200 OK and contact help data in English") {
            val (_, token) = savedClient("en")
            self.mockMvc.get("/api/contact-help") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.phone.schedule.days") { value("Mon-Sun") }
            }
        }
    }

    describe("GET /api/faq/categories") {
        it("returns 200 OK and a list of FAQ categories in Polish") {
            val (_, token) = savedClient("pl")
            self.mockMvc.get("/api/faq/categories") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(6) }
                jsonPath("$[0].id") { value("1") }
                jsonPath("$[0].title") { value("Faktury i płatności") }
                jsonPath("$[0].route") { value("billing") }
            }
        }

        it("returns 200 OK and a list of FAQ categories in English") {
            val (_, token) = savedClient("en")
            self.mockMvc.get("/api/faq/categories") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].title") { value("Billing and payments") }
            }
        }
    }

    describe("GET /api/faq/categories/{route}") {
        it("returns 200 OK and FAQ category details in Polish") {
            val (_, token) = savedClient("pl")
            self.mockMvc.get("/api/faq/categories/billing") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value("billing") }
                jsonPath("$.title") { value("Faktury i płatności") }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content.length()") { value(2) }
                jsonPath("$.content[0].id") { value("q1") }
                jsonPath("$.content[0].title") { value("Pierwsza faktura po zawarciu lub zmianie umowy") }
            }
        }

        it("returns 200 OK and FAQ category details in English") {
            val (_, token) = savedClient("en")
            self.mockMvc.get("/api/faq/categories/billing") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.title") { value("Billing and payments") }
                jsonPath("$.content[0].title") { value("First invoice after signing or changing the contract") }
            }
        }

        it("returns 404 for unknown category route") {
            val (_, token) = savedClient()
            self.mockMvc.get("/api/faq/categories/unknown-route") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }
    }

    describe("GET /api/faq/search") {
        it("returns 200 OK and filtered search results in Polish") {
            val (_, token) = savedClient("pl")
            self.mockMvc.get("/api/faq/search?q=faktura") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(1) } // "faktura" matches q1 in Polish
                jsonPath("$[0].id") { value("q1") }
                jsonPath("$[0].title") { value("Pierwsza faktura po zawarciu lub zmianie umowy") }
            }
        }

        it("returns 200 OK and filtered search results in English") {
            val (_, token) = savedClient("en")
            self.mockMvc.get("/api/faq/search?q=invoice") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(1) } // "invoice" matches q1 and q2 in English
                jsonPath("$[0].id") { value("q1") }
            }
        }

        it("returns empty list when no matches found") {
            val (_, token) = savedClient("pl")
            self.mockMvc.get("/api/faq/search?q=giberish") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
}
