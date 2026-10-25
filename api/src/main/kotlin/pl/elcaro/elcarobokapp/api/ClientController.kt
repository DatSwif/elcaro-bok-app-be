package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.elcaro.elcarobokapp.service.ClientResponse
import pl.elcaro.elcarobokapp.service.ClientService
import pl.elcaro.elcarobokapp.service.ClientUpdateRequest

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client profile management")
class ClientController(
    private val clientService: ClientService,
) {

    @GetMapping
    @Operation(summary = "Get current client profile", description = "Returns the profile of the currently authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns client profile data", content = [Content(schema = Schema(implementation = ClientResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getClient(): ResponseEntity<ClientResponse> =
        ResponseEntity.ok(clientService.getClientByClientNumber(authenticatedClientNumber()))

    @GetMapping("/phone-numbers")
    @Operation(summary = "Get current client phone numbers", description = "Returns all phone numbers belonging to the currently authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns the list of phone numbers")
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getClientPhoneNumbers(): ResponseEntity<List<String>> =
        ResponseEntity.ok(clientService.getPhonesByClientNumber(authenticatedClientNumber()))

    @PutMapping
    @Operation(summary = "Update current client profile", description = "Updates the profile fields of the currently authenticated client. Only provided fields are updated.")
    @ApiResponse(responseCode = "200", description = "Returns updated client profile data", content = [Content(schema = Schema(implementation = ClientResponse::class))])
    @ApiResponse(responseCode = "400", description = "Validation error", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun updateClient(@Valid @RequestBody request: ClientUpdateRequest): ResponseEntity<ClientResponse> =
        ResponseEntity.ok(clientService.updateClientByClientNumber(authenticatedClientNumber(), request))
}
