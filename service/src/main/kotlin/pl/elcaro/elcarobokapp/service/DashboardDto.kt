package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class DashboardResponse(
    val account: DashboardAccountDto,
    val messages: List<DashboardMessageDto>,
    val services: List<DashboardServiceDto>,
    val offers: List<DashboardOfferDto>
)

data class DashboardAccountDto(
    @get:JsonProperty("account_number") val accountNumber: String,
    val balance: BigDecimal
)

data class DashboardMessageDto(
    val id: Long,
    val title: String,
    val content: String
)

data class DashboardServiceDto(
    @get:JsonProperty("service_type") val serviceType: String,
    val activeCount: Int
)

data class DashboardOfferDto(
    val id: Long,
    val title: String,
    val subtitle: String?,
    @get:JsonProperty("image_url") val imageUrl: String?
)
