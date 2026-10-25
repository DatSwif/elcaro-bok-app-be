package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal

data class OfferResponse(
    val id: Long,
    val title: String,
    val subtitle: String,
    @JsonProperty("image_url") val imageUrl: String?,
    val category: String,
    val tags: List<String>,
    val featured: Boolean,
)

data class OfferDetailsResponse(
    val id: Long,
    val title: String,
    val subtitle: String,
    @JsonProperty("legal_note") val legalNote: String,
    val variants: List<OfferVariantResponse>,
)

data class OfferVariantResponse(
    val id: Long,
    val name: String,
    val pricing: PricingResponse,
    val blocks: List<Block>,
)

data class PricingResponse(
    @JsonProperty("regular_price") val regularPrice: BigDecimal,
    @JsonProperty("current_price") val currentPrice: BigDecimal,
    val period: String,
    @JsonProperty("promo_description") val promoDescription: String,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = PromoBadgeBlock::class, name = "PROMO_BADGE"),
    JsonSubTypes.Type(value = InternetServiceBlock::class, name = "INTERNET_SERVICE"),
    JsonSubTypes.Type(value = TvPackagesBlock::class, name = "TV_PACKAGES"),
    JsonSubTypes.Type(value = MobilePlanBlock::class, name = "MOBILE_PLAN"),
    JsonSubTypes.Type(value = PremiumAddonsBlock::class, name = "PREMIUM_ADDONS"),
    JsonSubTypes.Type(value = HardwareBlock::class, name = "HARDWARE"),
    JsonSubTypes.Type(value = ContractConditionsBlock::class, name = "CONTRACT_CONDITIONS"),
)
sealed class Block {
    abstract val type: String
}

data class PromoBadgeBlock(
    val headline: String,
    val subheadline: String,
) : Block() {
    override val type: String = "PROMO_BADGE"
}

data class InternetServiceBlock(
    val icon: String,
    @JsonProperty("service_type") val serviceType: String,
    @JsonProperty("primary_metric") val primaryMetric: String,
    @JsonProperty("secondary_metric") val secondaryMetric: String,
) : Block() {
    override val type: String = "INTERNET_SERVICE"
}

data class TvPackagesBlock(
    val icon: String,
    val title: String,
    @JsonProperty("primary_metric") val primaryMetric: String,
    @JsonProperty("secondary_metric") val secondaryMetric: String,
) : Block() {
    override val type: String = "TV_PACKAGES"
}

data class MobilePlanBlock(
    val icon: String,
    val title: String,
    @JsonProperty("data_limit") val dataLimit: String,
    @JsonProperty("calls_sms") val callsSms: String,
    val roaming: String,
) : Block() {
    override val type: String = "MOBILE_PLAN"
}

data class PremiumAddonsBlock(
    val title: String,
    @JsonProperty("is_included") val isIncluded: Boolean,
    val description: String,
    val addons: List<Any> = emptyList(),
) : Block() {
    override val type: String = "PREMIUM_ADDONS"
}

data class HardwareBlock(
    val title: String,
    val items: List<HardwareItem>,
) : Block() {
    override val type: String = "HARDWARE"
}

data class HardwareItem(
    val icon: String,
    val name: String,
    @JsonProperty("cost_note") val costNote: String,
    @JsonProperty("is_mandatory") val isMandatory: Boolean,
)

data class ContractConditionsBlock(
    val title: String,
    @JsonProperty("duration_months") val durationMonths: Int,
    @JsonProperty("upfront_fees") val upfrontFees: List<UpfrontFee>,
) : Block() {
    override val type: String = "CONTRACT_CONDITIONS"
}

data class UpfrontFee(
    val name: String,
    val amount: BigDecimal,
)
