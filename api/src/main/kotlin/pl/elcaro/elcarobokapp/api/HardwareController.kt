package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.LocaleResolver
import pl.elcaro.elcarobokapp.service.HardwareItemView
import pl.elcaro.elcarobokapp.service.ServiceService

@RestController
@RequestMapping("/api/hardware")
@Tag(name = "Hardware", description = "Hardware information retrieval")
class HardwareController(
    private val serviceService: ServiceService,
    private val localeResolver: LocaleResolver,
) {
    @GetMapping
    @Operation(summary = "Get all hardware", description = "Returns all hardware items associated with the authenticated client's services.")
    @ApiResponse(responseCode = "200", description = "Returns list of hardware items")
    fun getAllHardware(request: HttpServletRequest): ResponseEntity<List<HardwareItemView>> {
        val clientNumber = authenticatedClientNumber()
        val locale = localeResolver.resolveLocale(request)
        return ResponseEntity.ok(serviceService.getAllHardwareByClientNumber(clientNumber, locale))
    }

    @GetMapping("/{hardware_id}")
    @Operation(summary = "Get hardware by ID", description = "Returns a single hardware item. Only accessible if the hardware belongs to one of the authenticated client's services.")
    @ApiResponse(responseCode = "200", description = "Returns hardware details", content = [Content(schema = Schema(implementation = HardwareItemView::class))])
    @ApiResponse(responseCode = "404", description = "Hardware not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getHardwareById(
        @Parameter(description = "Client hardware ID") @PathVariable("hardware_id") id: Long,
        request: HttpServletRequest,
    ): ResponseEntity<HardwareItemView> {
        val clientNumber = authenticatedClientNumber()
        val locale = localeResolver.resolveLocale(request)
        return ResponseEntity.ok(serviceService.getHardwareById(id, clientNumber, locale))
    }
}
