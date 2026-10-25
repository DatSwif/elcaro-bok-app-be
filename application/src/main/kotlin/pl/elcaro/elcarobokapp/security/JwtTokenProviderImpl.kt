package pl.elcaro.elcarobokapp.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProviderImpl(
    @Value($$"${jwt.secret}") secret: String,
    @Value($$"${jwt.expiration-ms}") private val expirationMs: Long,
) : JwtTokenProvider {

    private val signingKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    override fun generateToken(clientNumber: String): String =
        Jwts.builder()
            .subject(clientNumber)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey)
            .compact()

    override fun extractClientNumber(token: String): String? =
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (e: JwtException) {
            null
        }

    override fun isTokenValid(token: String): Boolean =
        extractClientNumber(token) != null
}
