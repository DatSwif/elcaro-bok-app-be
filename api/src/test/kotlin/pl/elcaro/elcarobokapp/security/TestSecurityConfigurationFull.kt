package pl.elcaro.elcarobokapp.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import java.util.*
import javax.crypto.SecretKey

@TestConfiguration
@Profile("security-test")
class TestSecurityConfigurationFull {

    companion object {
        const val TEST_JWT_SECRET = "security-test-secret-key-extended"
        const val TEST_JWT_EXPIRATION_MS = 3_600_000L

        fun expiredToken(clientNumber: String = "C001"): String {
            val signingKey = Keys.hmacShaKeyFor(TEST_JWT_SECRET.toByteArray(Charsets.UTF_8))
            return Jwts.builder()
                .subject(clientNumber)
                .issuedAt(Date(System.currentTimeMillis() - 7_200_000))
                .expiration(Date(System.currentTimeMillis() - 3_600_000))
                .signWith(signingKey)
                .compact()
        }
    }

    @Bean
    fun jwtTokenProvider(): JwtTokenProvider {
        val signingKey: SecretKey = Keys.hmacShaKeyFor(TEST_JWT_SECRET.toByteArray(Charsets.UTF_8))
        return object : JwtTokenProvider {
            override fun generateToken(clientNumber: String): String =
                Jwts.builder()
                    .subject(clientNumber)
                    .issuedAt(Date())
                    .expiration(Date(System.currentTimeMillis() + TEST_JWT_EXPIRATION_MS))
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
    }

    @Bean
    fun jwtAuthenticationFilter(jwtTokenProvider: JwtTokenProvider): OncePerRequestFilter =
        object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain,
            ) {
                val header = request.getHeader("Authorization")
                if (header != null && header.startsWith("Bearer ")) {
                    val token = header.removePrefix("Bearer ").trim()
                    if (jwtTokenProvider.isTokenValid(token)) {
                        val clientNumber = jwtTokenProvider.extractClientNumber(token)
                        if (clientNumber != null) {
                            SecurityContextHolder.getContext().authentication =
                                UsernamePasswordAuthenticationToken(
                                    clientNumber, null,
                                    listOf(SimpleGrantedAuthority("ROLE_CLIENT"))
                                )
                        }
                    }
                }
                filterChain.doFilter(request, response)
            }
        }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: OncePerRequestFilter,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/privacy-policy", "/api/terms-of-service").permitAll()
                    .requestMatchers("/api/documents/{id}/text", "/api/documents/{id}/pdf").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
        return http.build()
    }

}
