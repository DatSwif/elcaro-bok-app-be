package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.GlobalMessageResponse
import pl.elcaro.elcarobokapp.service.MessagesService
import pl.elcaro.elcarobokapp.service.PersonalMessageResponse

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages", description = "Global and personal client messages")
@SecurityRequirement(name = "Bearer")
class MessagesController(
    private val messagesService: MessagesService,
) {

    @GetMapping("/global")
    @Operation(summary = "Get global messages list", description = "Returns all global messages for the authenticated client, with per-client read state.")
    @ApiResponse(responseCode = "200", description = "Returns list of global messages", content = [Content(array = ArraySchema(schema = Schema(implementation = GlobalMessageResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getGlobalMessages(locale: java.util.Locale): ResponseEntity<List<GlobalMessageResponse>> {
        return ResponseEntity.ok(messagesService.listGlobal(authenticatedClientNumber(), locale))
    }

    @PutMapping("/global/{message_id}")
    @Operation(summary = "Mark a global message as read")
    @ApiResponse(responseCode = "200", description = "Message marked as read")
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Message not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun markGlobalRead(@PathVariable("message_id") messageId: Long): ResponseEntity<Void> {
        messagesService.markGlobalRead(authenticatedClientNumber(), messageId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/global/{message_id}")
    @Operation(summary = "Delete a global message by id")
    @ApiResponse(responseCode = "200", description = "Message successfully deleted")
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Message not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun deleteGlobal(@PathVariable("message_id") messageId: Long): ResponseEntity<Void> {
        messagesService.deleteGlobal(authenticatedClientNumber(), messageId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/personal")
    @Operation(summary = "Get personal messages list", description = "Returns all personal messages addressed to the authenticated client.")
    @ApiResponse(responseCode = "200", description = "Returns list of personal messages", content = [Content(array = ArraySchema(schema = Schema(implementation = PersonalMessageResponse::class)))])
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getPersonalMessages(locale: java.util.Locale): ResponseEntity<List<PersonalMessageResponse>> {
        return ResponseEntity.ok(messagesService.listPersonal(authenticatedClientNumber(), locale))
    }

    @PutMapping("/personal/{message_id}")
    @Operation(summary = "Mark a personal message as read")
    @ApiResponse(responseCode = "200", description = "Message marked as read")
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Message not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun markPersonalRead(@PathVariable("message_id") messageId: Long): ResponseEntity<Void> {
        messagesService.markPersonalRead(authenticatedClientNumber(), messageId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/personal/{message_id}")
    @Operation(summary = "Delete a personal message by id")
    @ApiResponse(responseCode = "200", description = "Message successfully deleted")
    @ApiResponse(responseCode = "401", description = "Unauthorized, invalid or missing token", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Message not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun deletePersonal(@PathVariable("message_id") messageId: Long): ResponseEntity<Void> {
        messagesService.deletePersonal(authenticatedClientNumber(), messageId)
        return ResponseEntity.ok().build()
    }

    private fun authenticatedClientNumber(): String =
        org.springframework.security.core.context.SecurityContextHolder.getContext().authentication!!.name
}