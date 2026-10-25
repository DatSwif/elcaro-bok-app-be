package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class AddressView(
    @get:JsonProperty("address_id") val addressId: Long?,
    val address: String,
    @get:JsonProperty("contract_active_from") val contractActiveFrom: LocalDate,
    @get:JsonProperty("contract_active_until") val contractActiveUntil: LocalDate?,
)

data class ServiceListView(
    val id: Long?,
    @get:JsonProperty("address_id") val addressId: Long?,
    val name: String,
    val status: String,
    @get:JsonProperty("monthly_fee") val monthlyFee: BigDecimal?,
    @get:JsonProperty("service_type") val serviceType: String?,
)

data class ServiceDetailView(
    @get:JsonProperty("user_service_id") val userServiceId: Long?,
    @get:JsonProperty("address_id") val addressId: Long?,
    val name: String,
    val status: String,
    @get:JsonProperty("monthly_fee") val monthlyFee: BigDecimal?,
    @get:JsonProperty("service_type") val serviceType: String?,
    @get:JsonProperty("start_date") val startDate: LocalDate?,
    @get:JsonProperty("end_date") val endDate: LocalDate?,
    val description: String?,
    @get:JsonProperty("related_hardware") val relatedHardware: List<HardwareView>,
    val packages: List<TVChannelPackageView>,
    @get:JsonProperty("available_add_ons") val availableAddOns: List<TVChannelPackageView>? = null,
)

data class HardwareView(
    val id: Long?,
    val name: String,
    val imagePath: String? = null,
)

data class HardwareItemView(
    val id: Long?,
    val name: String,
    val serviceType: String?,
    val owner: String,
    val icon: String?,
    val serialNumber: String?,
    @get:JsonProperty("isReturnable") val isReturnable: Boolean,
    @get:JsonProperty("isReportable") val isReportable: Boolean,
)

data class TVChannelPackageView(
    val id: Long?,
    val name: String,
    @get:JsonProperty("monthly_fee") val monthlyFee: BigDecimal?,
    @get:JsonProperty("is_base_package") val isBasePackage: Boolean? = null,
    val channels: List<TVChannelView>,
)

data class TVChannelView(
    val name: String,
    val pictogram: String?,
)
