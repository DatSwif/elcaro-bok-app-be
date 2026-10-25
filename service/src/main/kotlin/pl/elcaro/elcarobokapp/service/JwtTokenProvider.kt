package pl.elcaro.elcarobokapp.service

interface JwtTokenProvider {
    fun generateToken(clientNumber: String): String
    fun extractClientNumber(token: String): String?
    fun isTokenValid(token: String): Boolean
}
