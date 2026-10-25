package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class MarketingConsentResponse(
    val code: String,
    val title: String,
    val description: String,
    @get:JsonProperty("isGranted") val isGranted: Boolean,
    @get:JsonProperty("isEditable") val isEditable: Boolean,
    val grantedAt: Instant?
)

data class ConsentUpdateRequest(
    @field:NotBlank(message = "Consent code is required")
    @field:Size(max = 100, message = "Consent code must not exceed 100 characters")
    val code: String,
    @param:JsonProperty("isGranted") @get:JsonProperty("isGranted") val isGranted: Boolean
)
