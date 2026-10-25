package pl.elcaro.elcarobokapp.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import pl.elcaro.elcarobokapp.domain.registration.Client
import pl.elcaro.elcarobokapp.domain.registration.UserNotificationSetting
import pl.elcaro.elcarobokapp.infrastructure.*
import pl.elcaro.elcarobokapp.service.JwtTokenProvider
import pl.elcaro.elcarobokapp.service.NotificationSettingDto
import pl.elcaro.elcarobokapp.service.NotificationSettingsWrapperDto
import tools.jackson.databind.ObjectMapper

class NotificationSettingControllerItTest : BaseIntegrationTest({

    val self = this as NotificationSettingControllerItTest

    beforeTest {
        self.userConsentRepository.deleteAll()
        self.userNotificationSettingRepository.deleteAll()
        self.notificationTypeRepository.deleteAll()
        self.clientServiceRepository.deleteAll()
        self.clientRepository.deleteAll()
    }

    fun savedClient(languageCode: String = "en"): Pair<Client, String> {
        val client = self.clientRepository.save(Client(clientNumber = "C001", languageCode = languageCode))
        val token = self.jwtTokenProvider.generateToken("C001")
        return client to token
    }

    describe("GET /api/clients/lang-notification-settings") {

        it("returns 200 with all notification types and client's settings") {
            val (client, token) = savedClient()
            val type = self.notificationTypeRepository.save(testNotificationTypeWithTranslations())
            self.userNotificationSettingRepository.save(
                UserNotificationSetting(client = client, notificationType = type, channel = "EMAIL", isEnabled = true)
            )

            self.mockMvc.get("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.language_code") { value("en") }
                jsonPath("$.notifications") { isArray() }
                jsonPath("$.notifications.length()") { value(3) } // 1 type * 3 channels
                
                // Assuming EMAIL is one of the channels returned
                jsonPath("$.notifications[?(@.channel == 'EMAIL')].notificationTypeCode") { value("EMAIL_PROMO") }
                jsonPath("$.notifications[?(@.channel == 'EMAIL')].notificationTypeName") { value("Promotional emails") }
                jsonPath("$.notifications[?(@.channel == 'EMAIL')].enabled") { value(true) }
                
                // Assuming SMS is returned and false
                jsonPath("$.notifications[?(@.channel == 'SMS')].enabled") { value(false) }
            }
        }

        it("returns translated names based on client's language") {
            val (_, token) = savedClient("pl")
            self.notificationTypeRepository.save(testNotificationTypeWithTranslations())

            self.mockMvc.get("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.language_code") { value("pl") }
                jsonPath("$.notifications[0].notificationTypeName") { value("Emaile promocyjne") }
            }
        }

        it("returns empty list when no notification types exist") {
            val (_, token) = savedClient()

            self.mockMvc.get("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.language_code") { value("en") }
                jsonPath("$.notifications") { isEmpty() }
            }
        }
    }

    describe("PUT /api/clients/lang-notification-settings") {

        it("creates new settings and returns updated list with language_code") {
            val (_, token) = savedClient()
            val type = self.notificationTypeRepository.save(testNotificationTypeWithTranslations())
            
            val wrapper = NotificationSettingsWrapperDto(
                languageCode = "fr",
                notifications = listOf(
                    NotificationSettingDto(
                        notificationTypeId = type.id!!,
                        notificationTypeCode = "",
                        notificationTypeName = "",
                        channel = "SMS",
                        enabled = true
                    )
                )
            )
            val body = self.objectMapper.writeValueAsString(wrapper)

            self.mockMvc.put("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = body
            }.andExpect {
                status { isOk() }
                jsonPath("$.language_code") { value("fr") }
                jsonPath("$.notifications[?(@.channel == 'SMS')].enabled") { value(true) }
            }
            
            // Verify client language was actually updated in DB
            val updatedClient = self.clientRepository.findByClientNumber("C001")
            assert(updatedClient?.languageCode == "fr")
        }

        it("updates existing settings") {
            val (client, token) = savedClient()
            val type = self.notificationTypeRepository.save(testNotificationTypeWithTranslations())
            self.userNotificationSettingRepository.save(
                UserNotificationSetting(client = client, notificationType = type, channel = "EMAIL", isEnabled = true)
            )
            
            val wrapper = NotificationSettingsWrapperDto(
                languageCode = "en",
                notifications = listOf(
                    NotificationSettingDto(
                        notificationTypeId = type.id!!,
                        notificationTypeCode = "",
                        notificationTypeName = "",
                        channel = "EMAIL",
                        enabled = false
                    )
                )
            )
            val body = self.objectMapper.writeValueAsString(wrapper)

            self.mockMvc.put("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = body
            }.andExpect {
                status { isOk() }
                jsonPath("$.language_code") { value("en") }
                jsonPath("$.notifications[?(@.channel == 'EMAIL')].enabled") { value(false) }
            }
        }

        it("returns 404 for invalid notification type ID") {
            val (_, token) = savedClient()
            
            val wrapper = NotificationSettingsWrapperDto(
                languageCode = "en",
                notifications = listOf(
                    NotificationSettingDto(
                        notificationTypeId = 999,
                        notificationTypeCode = "",
                        notificationTypeName = "",
                        channel = "SMS",
                        enabled = true
                    )
                )
            )
            val body = self.objectMapper.writeValueAsString(wrapper)

            self.mockMvc.put("/api/clients/lang-notification-settings") {
                accept = MediaType.APPLICATION_JSON
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = body
            }.andExpect {
                status { isNotFound() }
            }
        }
    }
}) {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var clientRepository: ClientRepository

    @Autowired
    lateinit var notificationTypeRepository: NotificationTypeRepository

    @Autowired
    lateinit var userNotificationSettingRepository: UserNotificationSettingRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    lateinit var userConsentRepository: UserConsentRepository

    @Autowired
    lateinit var clientServiceRepository: ClientServiceRepository
}
