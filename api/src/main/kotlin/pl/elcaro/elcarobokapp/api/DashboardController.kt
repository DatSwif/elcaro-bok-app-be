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
import pl.elcaro.elcarobokapp.service.DashboardResponse
import pl.elcaro.elcarobokapp.service.DashboardService

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Aggregated dashboard data for the authenticated client")
@SecurityRequirement(name = "Bearer")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Returns aggregated data for the dashboard, including account summary, recent messages, active services, and promoted offers.")
    @ApiResponse(responseCode = "200", description = "Success", content = [Content(schema = Schema(implementation = DashboardResponse::class))])
    @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid token)", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun getDashboardData(locale: java.util.Locale): ResponseEntity<DashboardResponse> {
        val dashboardData = dashboardService.getDashboardData(authenticatedClientNumber(), locale)
        return ResponseEntity.ok(dashboardData)
    }
}
