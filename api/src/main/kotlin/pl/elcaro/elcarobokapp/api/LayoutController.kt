package pl.elcaro.elcarobokapp.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.elcaro.elcarobokapp.service.LayoutResponse
import pl.elcaro.elcarobokapp.service.LayoutService

@RestController
@RequestMapping("/api/layout")
@Tag(name = "Layout", description = "Application layout and header data")
@SecurityRequirement(name = "Bearer")
class LayoutController(
    private val layoutService: LayoutService
) {

    @GetMapping
    @Operation(summary = "Get layout data", description = "Returns data for the application header, such as the authenticated client's name and message count.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(schema = Schema(implementation = LayoutResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getLayoutData(): ResponseEntity<LayoutResponse> {
        val layoutData = layoutService.getLayoutData(authenticatedClientNumber())
        return ResponseEntity.ok(layoutData)
    }
}
