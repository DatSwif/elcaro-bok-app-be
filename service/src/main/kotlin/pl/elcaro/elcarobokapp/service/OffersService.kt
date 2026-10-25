package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.offers.Offer
import pl.elcaro.elcarobokapp.domain.offers.OfferOrder
import pl.elcaro.elcarobokapp.domain.offers.OfferVariant
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.OfferOrderRepository
import pl.elcaro.elcarobokapp.infrastructure.OfferRepository
import pl.elcaro.elcarobokapp.infrastructure.OfferVariantRepository
import tools.jackson.databind.ObjectMapper
import java.util.*

@Service
@Transactional
class OffersService(
    private val offerRepository: OfferRepository,
    private val offerVariantRepository: OfferVariantRepository,
    private val offerOrderRepository: OfferOrderRepository,
    private val clientRepository: ClientRepository,
    private val mapper: ObjectMapper,
) {

    @Transactional(readOnly = true)
    fun listOffers(clientNumber: String? = null, locale: java.util.Locale): List<OfferResponse> {
        return offerRepository.findAllByOrderByIdAsc().map { it.toListResponse(locale) }
    }

    @Transactional(readOnly = true)
    fun getOfferDetails(offerId: Long, clientNumber: String? = null, locale: java.util.Locale): OfferDetailsResponse {
        val offer = offerRepository.findById(offerId).orElseThrow {
            NoSuchElementException("Offer not found")
        }
        
        val translation = offer.translations.firstOrNull { it.id.languageCode == locale.language }
            ?: offer.translations.firstOrNull { it.id.languageCode == "pl" }
            ?: offer.translations.firstOrNull()

        return OfferDetailsResponse(
            id = offer.id!!,
            title = translation?.title ?: "No title",
            subtitle = translation?.subtitle ?: "",
            legalNote = offer.legalNote,
            variants = offer.variants.map { it.toResponse() },
        )
    }

    fun placeOrder(clientNumber: String, offerId: Long, variantId: Long) {
        val variant = offerVariantRepository.findByIdAndOfferId(variantId, offerId)
            ?: throw NoSuchElementException("Variant not found for this offer")
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client not found")
        offerOrderRepository.save(OfferOrder(client = client, variant = variant))
    }

    private fun Offer.toListResponse(locale: Locale): OfferResponse {
        val translation = translations.firstOrNull { it.id.languageCode == locale.language }
            ?: translations.firstOrNull { it.id.languageCode == "pl" }
            ?: translations.firstOrNull()

        return OfferResponse(
            id = id!!,
            title = translation?.title ?: "No title",
            subtitle = translation?.subtitle ?: "",
            imageUrl = imageUrl,
            category = category.name,
            tags = if (tags.isBlank()) emptyList() else tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            featured = featured,
        )
    }

    private fun OfferVariant.toResponse(): OfferVariantResponse {
        val blocks: List<Block> = mapper.readValue(
            blocksJson,
            mapper.typeFactory.constructCollectionLikeType(List::class.java, Block::class.java),
        )
        return OfferVariantResponse(
            id = id!!,
            name = name,
            pricing = PricingResponse(
                regularPrice = regularPrice,
                currentPrice = currentPrice,
                period = period,
                promoDescription = promoDescription,
            ),
            blocks = blocks,
        )
    }
}
