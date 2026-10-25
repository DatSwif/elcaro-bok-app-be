package pl.elcaro.elcarobokapp.service

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class GlobalMessageResponse(
    val id: Long,
    val title: String,
    val content: String,
    @get:JsonProperty("is_read") val isRead: Boolean,
    @get:JsonProperty("read_at") val readAt: Instant?,
)

data class PersonalMessageResponse(
    val id: Long,
    val title: String,
    val level: Int,
    @get:JsonProperty("is_read") val isRead: Boolean,
    @get:JsonProperty("read_at") val readAt: Instant?,
)
