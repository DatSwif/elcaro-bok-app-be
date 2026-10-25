package pl.elcaro.elcarobokapp.service

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format")
    val phoneNumber: String,
)

data class VerifyCodeRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format")
    val phoneNumber: String,

    @field:NotBlank(message = "Code is required")
    @field:Pattern(regexp = "^\\d{4}$", message = "Code must be a 4-digit number")
    val code: String,
)

data class RegisterRequest(
    @field:NotBlank(message = "Client number is required")
    @field:Size(max = 30, message = "Client number must not exceed 30 characters")
    val clientNumber: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be in valid format")
    val email: String,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format")
    val phoneNumber: String,

    val marketingConsent: Boolean = false,
)

data class LoginResponse(
    val token: String,
)

data class CodeResponse(
    val code: String,
)
