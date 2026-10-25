package pl.elcaro.elcarobokapp.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.*

class AuthDocumentServiceTest : DescribeSpec({

    val service = AuthDocumentService()
    val pl = Locale.forLanguageTag("pl")
    val en = Locale.forLanguageTag("en")

    describe("getTermsOfService") {
        it("returns Polish content with type 'terms' for pl locale") {
            val result = service.getTermsOfService(pl)
            result.id shouldBe "t1"
            result.type shouldBe "terms"
            result.title shouldBe "Regulamin rejestracji"
            result.text shouldContain "Regulamin"
        }

        it("returns English content for en locale") {
            val result = service.getTermsOfService(en)
            result.title shouldBe "Registration terms"
            result.text shouldContain "Terms"
        }
    }

    describe("getPrivacyPolicy") {
        it("returns Polish content with type 'privacy' for pl locale") {
            val result = service.getPrivacyPolicy(pl)
            result.id shouldBe "p1"
            result.type shouldBe "privacy"
            result.title shouldBe "Polityka prywatności rejestracji"
        }

        it("returns English content for en locale") {
            val result = service.getPrivacyPolicy(en)
            result.title shouldBe "Registration privacy policy"
            result.text shouldContain "Privacy policy"
        }
    }

    describe("getMarketingConsents") {
        it("returns Polish content with type 'marketing-consents' for pl locale") {
            val result = service.getMarketingConsents(pl)
            result.id shouldBe "m1"
            result.type shouldBe "marketing-consents"
            result.title shouldBe "Zgody marketingowe"
        }

        it("returns English content for en locale") {
            val result = service.getMarketingConsents(en)
            result.title shouldBe "Marketing consents"
        }
    }

    describe("getPromotionsRegulations") {
        it("returns Polish content with type 'telecommunications-services' for pl locale") {
            val result = service.getPromotionsRegulations(pl)
            result.id shouldBe "d3"
            result.type shouldBe "telecommunications-services"
            result.title shouldContain "Regulamin promocji"
        }

        it("returns English content for en locale") {
            val result = service.getPromotionsRegulations(en)
            result.title shouldContain "Promotion terms"
        }
    }
})
