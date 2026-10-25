package pl.elcaro.elcarobokapp.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pl.elcaro.elcarobokapp.domain.offers.*
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.ClientPhone
import pl.elcaro.elcarobokapp.infrastructure.ClientPhoneRepository
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.OfferOrderRepository
import pl.elcaro.elcarobokapp.infrastructure.OfferRepository
import pl.elcaro.elcarobokapp.infrastructure.OfferVariantRepository
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import pl.elcaro.elcarobokapp.service.VerifyCodeRequest
import java.math.BigDecimal
import java.time.Instant

class OffersControllerItTest : BaseIntegrationTest({

    val self = this as OffersControllerItTest

    beforeTest {
        self.offerOrderRepository.deleteAll()
        self.offerVariantRepository.deleteAll()
        self.offerRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    afterTest {
        self.offerOrderRepository.deleteAll()
        self.offerVariantRepository.deleteAll()
        self.offerRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    fun savedClientWithToken(clientNumber: String = "C001", phone: String = "123456789"): Pair<Client, String> {
        val client = Client(clientNumber = clientNumber, languageCode = "pl")
        client.phones.add(
            ClientPhone(client = client, phone = phone, expiresAt = Instant.now().plusSeconds(86400), createdAt = Instant.now())
        )
        self.clientRepository.save(client)
        val token = self.jwtTokenProvider.generateToken(clientNumber)
        return client to token
    }

    fun requestOtpAndReadCode(offerId: Long?, variantId: Long?, token: String, phone: String = "123456789"): String {
        self.mockMvc.post("/api/offers/$offerId/variants/$variantId/init") {
            param("phoneNumber", phone)
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }
        return self.clientPhoneRepository.findByPhone(phone)!!.code!!
    }

    fun savedOffer(
        title: String = "Test Offer",
        subtitle: String = "Test Subtitle",
        category: OfferCategory = OfferCategory.INTERNET,
        tags: String = "PROMO",
        featured: Boolean = false,
    ): Offer {
        val offer = Offer(
            category = category,
            tags = tags,
            featured = featured,
            legalNote = "legal",
        )
        offer.translations.add(
            OfferTranslation(
                id = OfferTranslationId(languageCode = "pl", offerId = 0),
                offer = offer,
                title = title,
                subtitle = subtitle
            )
        )
        return self.offerRepository.save(offer)
    }

    fun savedVariant(
        offer: Offer,
        blocksJson: String = """[{"type":"PROMO_BADGE","headline":"H","subheadline":"S"}]""",
    ): OfferVariant = self.offerVariantRepository.save(
        OfferVariant(
            offer = offer,
            name = "Variant",
            regularPrice = BigDecimal("100.00"),
            currentPrice = BigDecimal("80.00"),
            period = "MONTH",
            promoDescription = "promo",
            blocksJson = blocksJson,
        )
    )

    describe("GET /api/offers") {
        it("returns 200 with all offers") {
            val (_, token) = savedClientWithToken()
            savedOffer(title = "A", subtitle = "Sub A", category = OfferCategory.INTERNET, tags = "PROMO,NEW", featured = true)
            savedOffer(title = "B", subtitle = "Sub B", category = OfferCategory.MOBILE, tags = "", featured = false)

            self.mockMvc.get("/api/offers") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].title") { value("A") }
                jsonPath("$[0].category") { value("INTERNET") }
                jsonPath("$[0].tags[0]") { value("PROMO") }
                jsonPath("$[0].tags[1]") { value("NEW") }
                jsonPath("$[0].featured") { value(true) }
                jsonPath("$[1].tags.length()") { value(0) }
            }
        }

        it("tolerates the format=json query parameter") {
            val (_, token) = savedClientWithToken()
            self.mockMvc.get("/api/offers?format=json") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/offers").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    describe("GET /api/offers/{offer_id}") {
        it("returns 200 with details, variants, and parsed blocks") {
            val (_, token) = savedClientWithToken()
            val offer = savedOffer()
            savedVariant(
                offer,
                blocksJson = """[
                    {"type":"PROMO_BADGE","headline":"H","subheadline":"S"},
                    {"type":"INTERNET_SERVICE","icon":"globe","service_type":"FIBER","primary_metric":"1Gb","secondary_metric":"down"},
                    {"type":"CONTRACT_CONDITIONS","title":"T","duration_months":24,"upfront_fees":[{"name":"a","amount":1.00}]}
                ]""".trimIndent()
            )

            self.mockMvc.get("/api/offers/${offer.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(offer.id!!.toInt()) }
                jsonPath("$.legal_note") { value("legal") }
                jsonPath("$.variants.length()") { value(1) }
                jsonPath("$.variants[0].pricing.regular_price") { value(100.00) }
                jsonPath("$.variants[0].pricing.current_price") { value(80.00) }
                jsonPath("$.variants[0].pricing.period") { value("MONTH") }
                jsonPath("$.variants[0].blocks.length()") { value(3) }
                jsonPath("$.variants[0].blocks[0].type") { value("PROMO_BADGE") }
                jsonPath("$.variants[0].blocks[1].type") { value("INTERNET_SERVICE") }
                jsonPath("$.variants[0].blocks[1].service_type") { value("FIBER") }
                jsonPath("$.variants[0].blocks[2].duration_months") { value(24) }
            }
        }

        it("round-trips a HARDWARE block with snake_case is_mandatory and cost_note") {
            val (_, token) = savedClientWithToken()
            val offer = savedOffer()
            savedVariant(
                offer,
                blocksJson = """[
                    {"type":"HARDWARE","title":"Sprzęt","items":[
                        {"icon":"router","name":"Router Wi-Fi 6","cost_note":"w cenie","is_mandatory":true},
                        {"icon":"set-top-box","name":"Dekoder 4K","cost_note":"w cenie","is_mandatory":false}
                    ]}
                ]""".trimIndent()
            )

            self.mockMvc.get("/api/offers/${offer.id}") {
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.variants[0].blocks[0].type") { value("HARDWARE") }
                jsonPath("$.variants[0].blocks[0].items.length()") { value(2) }
                jsonPath("$.variants[0].blocks[0].items[0].name") { value("Router Wi-Fi 6") }
                jsonPath("$.variants[0].blocks[0].items[0].cost_note") { value("w cenie") }
                jsonPath("$.variants[0].blocks[0].items[0].is_mandatory") { value(true) }
                jsonPath("$.variants[0].blocks[0].items[1].is_mandatory") { value(false) }
            }
        }

        it("returns 404 for unknown offer id") {
            val (_, token) = savedClientWithToken()
            self.mockMvc.get("/api/offers/9999") {
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.get("/api/offers/1").andExpect { status { isUnauthorized() } }
        }
    }

    describe("POST /api/offers/{offer_id}/variants/{variant_id}/verify") {
        it("returns 200 and persists the order") {
            val (_, token) = savedClientWithToken()
            val offer = savedOffer()
            val variant = savedVariant(offer)

            val code = requestOtpAndReadCode(offer.id, variant.id, token)

            self.mockMvc.post("/api/offers/${offer.id}/variants/${variant.id}/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber = "123456789", code = code))
                header("Authorization", "Bearer $token")
            }.andExpect { status { isOk() } }

            assert(self.offerOrderRepository.findAll().toList().size == 1)
        }

        it("returns 422 (not 401, which would log the user out) when the OTP code is invalid") {
            val (_, token) = savedClientWithToken()
            val offer = savedOffer()
            val variant = savedVariant(offer)

            requestOtpAndReadCode(offer.id, variant.id, token)

            self.mockMvc.post("/api/offers/${offer.id}/variants/${variant.id}/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber = "123456789", code = "000000"))
                header("Authorization", "Bearer $token")
            }.andExpect { status { isUnprocessableEntity() } }

            assert(self.offerOrderRepository.findAll().toList().isEmpty())
        }

        it("returns 404 if variant doesn't belong to the offer") {
            val (_, token) = savedClientWithToken()
            val offerA = savedOffer(title = "A", subtitle = "Sub A")
            val offerB = savedOffer(title = "B", subtitle = "Sub B")
            val variantOfB = savedVariant(offerB)

            val code = requestOtpAndReadCode(offerA.id, variantOfB.id, token)

            self.mockMvc.post("/api/offers/${offerA.id}/variants/${variantOfB.id}/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber = "123456789", code = code))
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        it("returns 404 for unknown variant") {
            val (_, token) = savedClientWithToken()
            val offer = savedOffer()

            val code = requestOtpAndReadCode(offer.id, 9999, token)

            self.mockMvc.post("/api/offers/${offer.id}/variants/9999/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = self.objectMapper.writeValueAsString(VerifyCodeRequest(phoneNumber = "123456789", code = code))
                header("Authorization", "Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        it("returns 401 when no token provided") {
            self.mockMvc.post("/api/offers/1/variants/1/verify").andExpect { status { isUnauthorized() } }
        }
    }
}) {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var clientRepository: ClientRepository
    @Autowired lateinit var clientPhoneRepository: ClientPhoneRepository
    @Autowired lateinit var offerRepository: OfferRepository
    @Autowired lateinit var offerVariantRepository: OfferVariantRepository
    @Autowired lateinit var offerOrderRepository: OfferOrderRepository
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
}
