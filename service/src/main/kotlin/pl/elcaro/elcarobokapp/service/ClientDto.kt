package pl.elcaro.elcarobokapp.service

import jakarta.validation.constraints.Size

data class ClientUpdateRequest(
    @field:Size(max = 50, message = "First name must not exceed 50 characters")
    val firstName: String? = null,

    @field:Size(max = 50, message = "Last name must not exceed 50 characters")
    val lastName: String? = null,

    @field:Size(max = 100, message = "Organization name must not exceed 100 characters")
    val organizationName: String? = null,

    @field:Size(max = 2, message = "Language code invalid")
    val languageCode: String? = null,

    val emails: List<String>? = null,
    val phones: List<String>? = null,
)

data class ClientResponse(
    val id: Long,
    val clientNumber: String?,
    val firstName: String?,
    val lastName: String?,
    val organizationName: String?,
    val languageCode: String,
    val emails: List<String>,
    val phones: List<String>
)
