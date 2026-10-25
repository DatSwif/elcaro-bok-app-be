package pl.elcaro.elcarobokapp.api

import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
