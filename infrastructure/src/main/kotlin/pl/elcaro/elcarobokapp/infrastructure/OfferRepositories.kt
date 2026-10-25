package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.offers.Offer
import pl.elcaro.elcarobokapp.domain.offers.OfferOrder
import pl.elcaro.elcarobokapp.domain.offers.OfferVariant

interface OfferRepository : CrudRepository<Offer, Long> {
    fun findAllByOrderByIdAsc(): List<Offer>
}

interface OfferVariantRepository : CrudRepository<OfferVariant, Long> {
    fun findByIdAndOfferId(id: Long, offerId: Long): OfferVariant?
}

interface OfferOrderRepository : CrudRepository<OfferOrder, Long>
