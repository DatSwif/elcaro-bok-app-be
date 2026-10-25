package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessage
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessageTranslation
import pl.elcaro.elcarobokapp.domain.messages.GlobalMessageTranslationId
import pl.elcaro.elcarobokapp.domain.offers.Offer
import pl.elcaro.elcarobokapp.domain.offers.OfferCategory
import pl.elcaro.elcarobokapp.domain.offers.OfferTranslation
import pl.elcaro.elcarobokapp.domain.offers.OfferTranslationId
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.services.ClientService
import pl.elcaro.elcarobokapp.domain.services.Service
import pl.elcaro.elcarobokapp.infrastructure.*
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import java.math.BigDecimal
import java.time.Instant

class DashboardControllerItTest : BaseIntegrationTest({

    val self = this as DashboardControllerItTest

    beforeTest {
        self.globalMessageRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.serviceRepository.deleteAll()
        self.offerRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    fun savedClientWithToken(clientNumber: String = "C001"): Pair<Client, String> {
        val client = self.clientRepository.save(
            Client(clientNumber = clientNumber, languageCode = "pl")
        )
        val token = self.jwtTokenProvider.generateToken(clientNumber)
        return client to token
    }

    describe("GET /api/dashboard") {

        it("returns 200 OK and correctly formatted dashboard data") {
            val (client, token) = savedClientWithToken()
            
            // 1. Setup Messages (create 4, expect only 3 back)
            for (i in 1..4) {
                val msg = GlobalMessage(createdAt = Instant.now())
                msg.translations.add(
                    GlobalMessageTranslation(
                        id = GlobalMessageTranslationId(languageCode = "pl", globalMessageId = 0),
                        globalMessage = msg,
                        title = "Message $i",
                        content = "Content $i"
                    )
                )
                self.globalMessageRepository.save(msg)
            }

            // 2. Setup Services (2 active INTERNET, 1 inactive TV)
            val service = self.serviceRepository.save(Service(monthlyFee = BigDecimal("49.99")))
            self.clientServiceRepository.save(
                ClientService(service = service, client = client, status = "ACTIVE", serviceType = "INTERNET")
            )
            self.clientServiceRepository.save(
                ClientService(service = service, client = client, status = "ACTIVE", serviceType = "INTERNET")
            )
            self.clientServiceRepository.save(
                ClientService(service = service, client = client, status = "INACTIVE", serviceType = "TV")
            )

            // 3. Setup Offers (create 3, expect only 2 back)
            for (i in 1..3) {
                val offer = Offer(
                    category = OfferCategory.INTERNET,
                    featured = true,
                    imageUrl = "/img/offer$i.png"
                )
                offer.translations.add(
                    OfferTranslation(
                        id = OfferTranslationId(languageCode = "pl", offerId = 0),
                        offer = offer,
                        title = "Test Offer $i",
                        subtitle = "Subtitle $i"
                    )
                )
                self.offerRepository.save(offer)
            }

            self.mockMvc.get("/api/dashboard") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                
                // Account
                jsonPath("$.account.account_number") { exists() }
                jsonPath("$.account.balance") { value(0) }
                
                // Messages
                jsonPath("$.messages") { isArray() }
                jsonPath("$.messages.length()") { value(4) }
                jsonPath("$.messages[0].title") { exists() }
                jsonPath("$.messages[0].content") { exists() }
                
                // Services
                jsonPath("$.services") { isArray() }
                jsonPath("$.services.length()") { value(2) } // INTERNET and TV
                jsonPath("$.services[?(@.service_type == 'INTERNET')].activeCount") { value(2) }
                jsonPath("$.services[?(@.service_type == 'TV')].activeCount") { value(1) }
                
                // Offers
                jsonPath("$.offers") { isArray() }
                jsonPath("$.offers.length()") { value(2) }
                jsonPath("$.offers[0].image_url") { exists() }
            }
        }

        it("returns 401 when no token is provided") {
            self.mockMvc.get("/api/dashboard") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var globalMessageRepository: GlobalMessageRepository
    @Autowired lateinit var clientServiceRepository: ClientServiceRepository
    @Autowired lateinit var serviceRepository: ServiceRepository
    @Autowired lateinit var offerRepository: OfferRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
}
