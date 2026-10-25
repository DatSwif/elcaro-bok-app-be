package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.services.ClientHardware
import pl.elcaro.elcarobokapp.domain.services.Hardware
import pl.elcaro.elcarobokapp.domain.services.Service
import pl.elcaro.elcarobokapp.infrastructure.*
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import java.math.BigDecimal
import pl.elcaro.elcarobokapp.domain.services.ClientService as ClientServiceEntity

class HardwareControllerItTest : BaseIntegrationTest({

    val self = this as HardwareControllerItTest

    beforeTest {
        self.clientHardwareRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.serviceRepository.deleteAll()
        self.hardwareRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    afterSpec {
        self.clientHardwareRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.serviceRepository.deleteAll()
        self.hardwareRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    describe("GET /api/hardware") {

        it("returns empty list when client has no hardware") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
        }

        it("returns hardware items for the authenticated client") {
            val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val hw1 = self.hardwareRepository.save(Hardware(imagePath = "/img/router.png", isReturnable = true, isReportable = true))
            val hw2 = self.hardwareRepository.save(Hardware(imagePath = "/img/decoder.png"))
            val service = self.serviceRepository.save(Service(monthlyFee = BigDecimal("49.99")))
            val cs = self.clientServiceRepository.save(
                ClientServiceEntity(service = service, client = client, status = "ACTIVE", serviceType = "fiber-optic-internet")
            )
            self.clientHardwareRepository.save(ClientHardware(hardware = hw1, clientService = cs, ownership = "elcaro"))
            self.clientHardwareRepository.save(ClientHardware(hardware = hw2, clientService = cs, ownership = "user", serialNumber = "SN-12345"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { isNotEmpty() }
                jsonPath("$[0].icon") { value("/img/router.png") }
                jsonPath("$[0].owner") { value("elcaro") }
                jsonPath("$[0].serviceType") { value("fiber-optic-internet") }
                jsonPath("$[0].isReturnable") { value(true) }
                jsonPath("$[0].isReportable") { value(true) }
            }
        }

        it("does not return hardware belonging to another client") {
            val client1 = self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val client2 = self.clientRepository.save(Client(clientNumber = "C002", languageCode = "pl"))
            val hw = self.hardwareRepository.save(Hardware(imagePath = "/img/router.png"))
            val service = self.serviceRepository.save(Service(monthlyFee = BigDecimal("49.99")))
            val cs = self.clientServiceRepository.save(
                ClientServiceEntity(service = service, client = client2, status = "ACTIVE")
            )
            self.clientHardwareRepository.save(ClientHardware(hardware = hw, clientService = cs, ownership = "elcaro"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
        }
    }

    describe("GET /api/hardware/{hardware_id}") {

        it("returns 404 when the hardware does not exist") {
            self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware/999") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }

        it("returns 404 when the hardware exists but belongs to another client") {
            val client1 = self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val client2 = self.clientRepository.save(Client(clientNumber = "C002", languageCode = "pl"))
            val hw = self.hardwareRepository.save(Hardware(imagePath = "/img/router.png"))
            val service = self.serviceRepository.save(Service(monthlyFee = BigDecimal("49.99")))
            val cs = self.clientServiceRepository.save(
                ClientServiceEntity(service = service, client = client2, status = "ACTIVE")
            )
            val clientHw = self.clientHardwareRepository.save(ClientHardware(hardware = hw, clientService = cs, ownership = "elcaro"))
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware/${clientHw.id}") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isNotFound() }
            }
        }

        it("returns the hardware when it belongs to the authenticated client") {
            val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = "pl"))
            val hw = self.hardwareRepository.save(Hardware(imagePath = "/img/router.png", isReturnable = true))
            val service = self.serviceRepository.save(Service(monthlyFee = BigDecimal("49.99")))
            val cs = self.clientServiceRepository.save(
                ClientServiceEntity(service = service, client = client, status = "ACTIVE", serviceType = "fiber-optic-internet")
            )
            val clientHw = self.clientHardwareRepository.save(
                ClientHardware(hardware = hw, clientService = cs, ownership = "elcaro", serialNumber = "SN-99999")
            )
            val token = self.jwtTokenProvider.generateToken("C001")

            self.mockMvc.get("/api/hardware/${clientHw.id}") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(clientHw.id) }
                jsonPath("$.icon") { value("/img/router.png") }
                jsonPath("$.owner") { value("elcaro") }
                jsonPath("$.serialNumber") { value("SN-99999") }
                jsonPath("$.serviceType") { value("fiber-optic-internet") }
                jsonPath("$.isReturnable") { value(true) }
                jsonPath("$.isReportable") { value(false) }
            }
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var serviceRepository: ServiceRepository

    @Autowired
    lateinit var clientServiceRepository: ClientServiceRepository

    @Autowired
    lateinit var hardwareRepository: HardwareRepository

    @Autowired
    lateinit var clientHardwareRepository: ClientHardwareRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider
}
