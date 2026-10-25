package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.elcaro.elcarobokapp.service.*
import java.util.*

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Service information retrieval")
class ServiceController(
    private val serviceService: ServiceService,
    private val clientService: ClientService,
    private val emailService: EmailService,
    private val authenticationService: AuthenticationService,
) {
    @GetMapping
    @Operation(summary = "Get all services", description = "Returns all services assigned to the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns list of services")
    fun getServices(): ResponseEntity<List<ServiceListView>> {
        val clientNumber = authenticatedClientNumber()
        val locale = Locale.of(clientService.getLanguageCode(clientNumber) ?: "pl")
        return ResponseEntity.ok(serviceService.getServicesByClientNumber(clientNumber, locale))
    }

    @GetMapping("/{service_id}")
    @Operation(summary = "Get service by ID", description = "Returns detailed service information including hardware and TV packages. Localized to the client's language preference.")
    @ApiResponse(responseCode = "200", description = "Returns detailed service info", content = [Content(schema = Schema(implementation = ServiceDetailView::class))])
    @ApiResponse(responseCode = "404", description = "Service not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getServiceById(@Parameter(description = "User service ID (client_service ID)") @PathVariable("service_id") id: Long): ResponseEntity<ServiceDetailView> {
        val clientNumber = authenticatedClientNumber()
        val languageCode = clientService.getLanguageCode(clientNumber)
        val locale = Locale.of(languageCode ?: "pl")
        return ResponseEntity.ok(serviceService.getServiceById(id, clientNumber, locale))
    }

    @GetMapping("/{service_id}/packages")
    @Operation(summary = "Get all TV channel packages for a service", description = "Returns all TV channel packages (base and add-ons) for the given service. Only available for TV services — returns 404 for internet or phone services.")
    @ApiResponse(responseCode = "200", description = "Returns list of TV channel packages")
    @ApiResponse(responseCode = "404", description = "Service not found or has no packages", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getAllPackagesForService(
        @Parameter(description = "Service ID") @PathVariable("service_id") id: Long,
    ): ResponseEntity<List<TVChannelPackageView>> {
        val clientNumber = authenticatedClientNumber()
        val locale = Locale.of(clientService.getLanguageCode(clientNumber) ?: "pl")
        return ResponseEntity.ok(serviceService.getAllPackagesByService(id, clientNumber, locale))
    }

    @GetMapping("/{service_id}/packages/{package_id}")
    @Operation(summary = "Get a TV channel package by ID", description = "Returns a single TV channel package for the given service. Only accessible if the service belongs to the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns the TV channel package")
    @ApiResponse(responseCode = "404", description = "Service or package not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getChannelPackageById(
        @Parameter(description = "Service ID") @PathVariable("service_id") id: Long,
        @Parameter(description = "Package ID") @PathVariable("package_id") packageId: Long,
    ): ResponseEntity<TVChannelPackageView> {
        val clientNumber = authenticatedClientNumber()
        val locale = Locale.of(clientService.getLanguageCode(clientNumber) ?: "pl")
        return ResponseEntity.ok(serviceService.getChannelPackageById(packageId, id, clientNumber, locale))
    }

    @GetMapping("/addresses")
    @Operation(summary = "Get service addresses", description = "Returns all addresses associated with the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns list of addresses")
    fun getAddresses(): ResponseEntity<List<AddressView>> =
        ResponseEntity.ok(serviceService.getAddressesByClientNumber(authenticatedClientNumber()))

    @PostMapping("/{service_id}/packages/{package_id}/init")
    @Operation(summary = "Initialize two-step package subscription")
    @ApiResponse(responseCode = "200", description = "OTP sent successfully")
    fun initPackageSubscription(
        @Parameter(description = "Service ID") @PathVariable("service_id") id: Long,
        @Parameter(description = "Package ID") @PathVariable("package_id") packageId: Long,
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

    @PostMapping("/{service_id}/packages/{package_id}/verify")
    @Operation(summary = "Verify OTP and subscribe to a TV channel package", description = "Subscribes the authenticated client to the given TV channel package.")
    @ApiResponse(responseCode = "200", description = "Subscribed successfully")
    @ApiResponse(responseCode = "404", description = "Service or package not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun verifyAndSubscribeToPackage(
        @Parameter(description = "Service ID") @PathVariable("service_id") id: Long,
        @Parameter(description = "Package ID") @PathVariable("package_id") packageId: Long,
        @RequestBody request: VerifyCodeRequest
    ): ResponseEntity<Void> {
        authenticationService.verifyOtp(request)
        val clientNumber = authenticatedClientNumber()
        serviceService.subscribeToPackage(clientNumber, id, packageId)
        clientService.getEmailsByClientNumber(clientNumber)
            .forEach { emailService.sendDummyEmail(it) }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{service_id}/packages/{package_id}")
    @Operation(summary = "Unsubscribe from a TV channel package", description = "Unsubscribes the authenticated client from the given TV channel package. Idempotent — unsubscribing when not subscribed has no effect.")
    @ApiResponse(responseCode = "200", description = "Unsubscribed successfully")
    @ApiResponse(responseCode = "404", description = "Service not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun unsubscribeFromPackage(
        @Parameter(description = "Service ID") @PathVariable("service_id") id: Long,
        @Parameter(description = "Package ID") @PathVariable("package_id") packageId: Long,
    ): ResponseEntity<Void> {
        serviceService.unsubscribeFromPackage(authenticatedClientNumber(), id, packageId)
        return ResponseEntity.ok().build()
    }
}
