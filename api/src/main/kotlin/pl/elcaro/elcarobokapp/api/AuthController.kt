package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import pl.elcaro.elcarobokapp.service.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "OTP-based authentication and token management")
@Validated
class AuthController(
    private val authenticationService: AuthenticationService,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @PostMapping("/login")
    @Operation(summary = "Request login OTP code", description = "Sends a one-time password via SMS to the provided phone number. Returns 200 regardless of whether the phone exists to prevent enumeration.")
    @ApiResponse(responseCode = "200", description = "Request processed, no content returned", content = [Content(schema = Schema(hidden = true))])
    @ApiResponse(responseCode = "429", description = "Account locked due to too many failed attempts", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<Void> {
        authenticationService.requestCode(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/register")
    @Operation(summary = "Request registration OTP code", description = "Initiates registration by sending an OTP code to the phone number. Client must already exist with matching client number, phone and email. Returns 200 regardless of validity to prevent enumeration.")
    @ApiResponse(responseCode = "200", description = "Request processed, no content returned", content = [Content(schema = Schema(hidden = true))])
    @ApiResponse(responseCode = "429", description = "Account locked due to too many failed attempts", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Void> {
        authenticationService.requestRegistrationCode(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/verify-code")
    @Operation(summary = "Verify OTP code", description = "Validates the OTP code and returns a JWT token on success. Activates the account if the client status is NEW.")
    @ApiResponse(responseCode = "200", description = "Code verified, returns JWT token", content = [Content(schema = Schema(implementation = LoginResponse::class))])
    @ApiResponse(responseCode = "401", description = "Invalid or expired code", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "429", description = "Account locked due to too many failed attempts", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun verifyCode(@Valid @RequestBody request: VerifyCodeRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(authenticationService.verifyCode(request))

    @GetMapping("/dev/code")
    @Operation(
        summary = "Get the current OTP code for a phone number (dev only)",
        description = "Returns the OTP code currently stored for the given phone number. Available only when app.dev.test-login.enabled=true. Returns 404 when the flag is disabled, no client matches the phone, or no code has been generated.",
    )
    @ApiResponse(responseCode = "200", description = "Returns the current OTP code", content = [Content(schema = Schema(implementation = CodeResponse::class))])
    @ApiResponse(responseCode = "404", description = "Endpoint disabled or no code available", content = [Content(schema = Schema(hidden = true))])
    fun getDevCode(
        @RequestParam("phoneNumber")
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format")
        phoneNumber: String,
    ): ResponseEntity<CodeResponse> {
        val code = authenticationService.findCodeForPhone(phoneNumber)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CodeResponse(code))
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate JWT token", description = "Checks whether the provided Bearer token is valid and not expired.")
    @ApiResponse(responseCode = "200", description = "Token is valid, no content returned", content = [Content(schema = Schema(hidden = true))])
    @ApiResponse(responseCode = "401", description = "Token is invalid or expired, no content returned", content = [Content(schema = Schema(hidden = true))])
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Void> {
        val token = authHeader.removePrefix("Bearer ").trim()
        if (token.isBlank() || !jwtTokenProvider.isTokenValid(token)) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.ok().build()
    }
}
