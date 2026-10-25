package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class NotificationSettingsWrapperDto(
    @field:NotBlank(message = "Language code is required")
    @field:Size(min = 2, max = 5, message = "Language code must be between 2 and 5 characters")
    @get:JsonProperty("language_code") val languageCode: String,

    @field:Valid
    val notifications: List<NotificationSettingDto>
)

data class NotificationSettingDto(
    @field:Positive(message = "Notification type ID must be positive")
    val notificationTypeId: Int,

    @field:Size(max = 100, message = "Notification type code must not exceed 100 characters")
    val notificationTypeCode: String,

    val notificationTypeName: String,
    val channel: String?,
    val enabled: Boolean,
)

data class NotificationSettingUpdateRequest(
    @field:Positive(message = "Notification type ID must be positive")
    val notificationTypeId: Int,
    val channel: String? = null,
    val enabled: Boolean,
)
