package pl.elcaro.elcarobokapp.api

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import pl.elcaro.elcarobokapp.service.AccountLockedException
import pl.elcaro.elcarobokapp.service.AuthenticationException
import pl.elcaro.elcarobokapp.service.ContactField
import pl.elcaro.elcarobokapp.service.DuplicateContactException
import pl.elcaro.elcarobokapp.service.InvalidOtpException

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(404)
            .body(ErrorResponse(status = 404, message = e.message ?: "Resource not found"))

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(e: AuthenticationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(401)
            .body(ErrorResponse(status = 401, message = "Invalid credentials"))

    @ExceptionHandler(InvalidOtpException::class)
    fun handleInvalidOtp(e: InvalidOtpException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(422)
            .body(ErrorResponse(status = 422, message = "Invalid verification code"))

    @ExceptionHandler(AccountLockedException::class)
    fun handleAccountLocked(e: AccountLockedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(429)
            .body(ErrorResponse(status = 429, message = "Too many failed attempts. Please try again later."))

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, message = message))
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException::class)
    fun handleConstraintViolation(e: jakarta.validation.ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = e.constraintViolations
            .joinToString("; ") { "${it.propertyPath.last()}: ${it.message}" }
        return ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, message = message))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, message = e.message ?: "Bad request"))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, message = e.message ?: "Invalid argument"))

    @ExceptionHandler(DuplicateContactException::class)
    fun handleDuplicateContact(e: DuplicateContactException): ResponseEntity<ErrorResponse> {
        val message = when (e.field) {
            ContactField.PHONE -> "Phone number is already in use"
            ContactField.EMAIL -> "Email address is already in use"
        }
        return ResponseEntity.status(409).body(ErrorResponse(status = 409, message = message))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.warn("Data integrity violation", e)
        return ResponseEntity.status(409)
            .body(ErrorResponse(status = 409, message = "Resource already exists"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return ResponseEntity.status(500)
            .body(ErrorResponse(status = 500, message = "Internal server error"))
    }
}
