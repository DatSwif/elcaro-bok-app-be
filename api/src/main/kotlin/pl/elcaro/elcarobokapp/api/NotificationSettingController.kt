package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.NotificationSettingService
import pl.elcaro.elcarobokapp.service.NotificationSettingsWrapperDto

@RestController
@RequestMapping("/api/clients/lang-notification-settings")
@Tag(name = "Notification Settings", description = "Client notification preferences management")
class NotificationSettingController(
    private val notificationSettingService: NotificationSettingService,
    private val localeResolver: LocaleResolver,
) {

    @GetMapping
    @Operation(summary = "Get notification settings", description = "Returns all notification types with the current client's enabled/disabled preferences and channel configuration.")
    @ApiResponse(responseCode = "200", description = "Returns list of notification settings per type", content = [Content(schema = Schema(implementation = NotificationSettingsWrapperDto::class))])
    @ApiResponse(responseCode = "404", description = "Client not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getNotificationSettings(request: HttpServletRequest): ResponseEntity<NotificationSettingsWrapperDto> =
        ResponseEntity.ok(
            notificationSettingService.getNotificationSettings(
                authenticatedClientNumber(),
                localeResolver.resolveLocale(request),
            )
        )

    @PutMapping
    @Operation(summary = "Update notification settings", description = "Bulk-updates notification preferences for the current client. Each entry specifies a notification type ID, channel, and enabled flag.")
    @ApiResponse(responseCode = "200", description = "Returns the updated list of notification settings", content = [Content(schema = Schema(implementation = NotificationSettingsWrapperDto::class))])
    @ApiResponse(responseCode = "404", description = "Client or notification type not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun updateNotificationSettings(
        @Valid @RequestBody wrapper: NotificationSettingsWrapperDto,
        request: HttpServletRequest,
    ): ResponseEntity<NotificationSettingsWrapperDto> =
        ResponseEntity.ok(
            notificationSettingService.updateNotificationSettings(
                authenticatedClientNumber(),
                wrapper,
                localeResolver.resolveLocale(request),
            )
        )
}
