package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.elcaro.elcarobokapp.service.*

@RestController
@RequestMapping("/api/offers")
@Tag(name = "Offers", description = "Marketing offers, variants, and ordering")
@SecurityRequirement(name = "Bearer")
class OffersController(
    private val offersService: OffersService,
    private val authenticationService: AuthenticationService,
    private val clientService: ClientService,
    private val emailService: EmailService,
) {

    @GetMapping
    @Operation(summary = "List offers", description = "Returns all offers shown on the offers main page.")
    @ApiResponse(responseCode = "200", description = "List of offers", content = [Content(array = ArraySchema(schema = Schema(implementation = OfferResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun listOffers(locale: java.util.Locale): ResponseEntity<List<OfferResponse>> =
        ResponseEntity.ok(offersService.listOffers(authenticatedClientNumber(), locale))

    @GetMapping("/{offer_id}")
    @Operation(summary = "Get offer details", description = "Returns offer details including variants and blocks.")
    @ApiResponse(responseCode = "200", description = "Offer details", content = [Content(schema = Schema(implementation = OfferDetailsResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Offer not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getOffer(@PathVariable("offer_id") offerId: Long, locale: java.util.Locale): ResponseEntity<OfferDetailsResponse> =
        ResponseEntity.ok(offersService.getOfferDetails(offerId, authenticatedClientNumber(), locale))

    @PostMapping("/{offer_id}/variants/{variant_id}/init")
    @Operation(summary = "Initialize two-step order verification")
    @ApiResponse(responseCode = "200", description = "OTP sent successfully")
    fun initOrderVerification(
        @PathVariable("offer_id") offerId: Long,
        @PathVariable("variant_id") variantId: Long,
        @RequestHeader(value = "secondaryPhone", defaultValue = "false", required = false) secondaryPhone: Boolean,
        @RequestParam(required = false) phoneNumber: String?
    ): ResponseEntity<Void> {
        val clientNumber = authenticatedClientNumber()
        val phone = phoneNumber ?: clientService.getPhonesByClientNumber(clientNumber).firstOrNull()
        if (phone != null) {
            authenticationService.requestOtp(phone)
        }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{offer_id}/variants/{variant_id}/verify")
    @Operation(summary = "Verify OTP and place an order for an offer variant")
    @ApiResponse(responseCode = "200", description = "Order placed")
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Offer or variant not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun verifyAndPlaceOrder(
        @PathVariable("offer_id") offerId: Long,
        @PathVariable("variant_id") variantId: Long,
        @RequestBody request: VerifyCodeRequest
    ): ResponseEntity<Void> {
        authenticationService.verifyOtp(request)
        val clientNumber = authenticatedClientNumber()
        offersService.placeOrder(clientNumber, offerId, variantId)
        clientService.getEmailsByClientNumber(clientNumber)
            .forEach { emailService.sendDummyEmail(it) }
        return ResponseEntity.ok().build()
    }
}
