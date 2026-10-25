package pl.elcaro.elcarobokapp.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.elcaro.elcarobokapp.domain.registration.*
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.NotificationTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserNotificationSettingRepository
import java.util.*

class NotificationSettingServiceTest : DescribeSpec({

    val clientRepository = mockk<ClientRepository>()
    val notificationTypeRepository = mockk<NotificationTypeRepository>()
    val userNotificationSettingRepository = mockk<UserNotificationSettingRepository>()
    val service = NotificationSettingService(clientRepository, notificationTypeRepository, userNotificationSettingRepository)

    beforeEach { clearAllMocks() }

    val clientNumber = "C001"

    fun client() = Client(clientNumber = clientNumber, languageCode = "pl").also { it.id = 1L }

    fun notificationType(id: Int, code: String, enName: String, plName: String): NotificationType {
        val type = NotificationType(code = code)
        type.id = id
        type.translations = mutableListOf(
            NotificationTypeTranslation(
                id = NotificationTypeTranslationId("en", id),
                notificationType = type,
                name = enName,
            ),
            NotificationTypeTranslation(
                id = NotificationTypeTranslationId("pl", id),
                notificationType = type,
                name = plName,
            ),
        )
        return type
    }

    fun setting(client: Client, type: NotificationType, enabled: Boolean, channel: String? = null): UserNotificationSetting {
        val s = UserNotificationSetting(client = client, notificationType = type, channel = channel, isEnabled = enabled)
        s.id = type.id!!.toLong()
        return s
    }

    describe("getNotificationSettings") {

        it("returns all notification types with client's settings merged") {
            val c = client()
            val type1 = notificationType(1, "EMAIL_PROMO", "Promotional emails", "Emaile promocyjne")
            val type2 = notificationType(2, "SMS_ALERTS", "SMS alerts", "Alerty SMS")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { notificationTypeRepository.findAll() } returns listOf(type1, type2)
            every { userNotificationSettingRepository.findByClientId(1L) } returns listOf(
                setting(c, type1, enabled = true, channel = "EMAIL"),
            )

            val result = service.getNotificationSettings(clientNumber, Locale.ENGLISH)

            result.notifications.size shouldBe 6 // 2 types * 3 channels
            val firstSetting = result.notifications.first { it.notificationTypeId == 1 && it.channel == "EMAIL" }
            firstSetting.notificationTypeName shouldBe "Promotional emails"
            firstSetting.enabled shouldBe true
            
            val secondSetting = result.notifications.first { it.notificationTypeId == 2 && it.channel == "SMS" }
            secondSetting.notificationTypeName shouldBe "SMS alerts"
            secondSetting.enabled shouldBe false
        }

        it("translates notification type name based on locale") {
            val c = client()
            val type = notificationType(1, "EMAIL_PROMO", "Promotional emails", "Emaile promocyjne")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { notificationTypeRepository.findAll() } returns listOf(type)
            every { userNotificationSettingRepository.findByClientId(1L) } returns emptyList()

            val result = service.getNotificationSettings(clientNumber, Locale.forLanguageTag("pl"))

            result.notifications.first().notificationTypeName shouldBe "Emaile promocyjne"
        }

        it("falls back to Polish when locale translation is not found") {
            val c = client()
            val type = notificationType(1, "EMAIL_PROMO", "Promotional emails", "Emaile promocyjne")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { notificationTypeRepository.findAll() } returns listOf(type)
            every { userNotificationSettingRepository.findByClientId(1L) } returns emptyList()

            val result = service.getNotificationSettings(clientNumber, Locale.FRENCH)

            result.notifications.first().notificationTypeName shouldBe "Emaile promocyjne"
        }

        it("throws NoSuchElementException when client not found") {
            every { clientRepository.findByClientNumber(clientNumber) } returns null

            shouldThrow<NoSuchElementException> {
                service.getNotificationSettings(clientNumber, Locale.ENGLISH)
            }
        }

        it("returns empty list when no notification types exist") {
            val c = client()
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { notificationTypeRepository.findAll() } returns emptyList()
            every { userNotificationSettingRepository.findByClientId(1L) } returns emptyList()

            val result = service.getNotificationSettings(clientNumber, Locale.ENGLISH)

            result.notifications shouldBe emptyList()
        }
    }

    describe("updateNotificationSettings") {

        it("creates new settings when none exist") {
            val c = client()
            val type = notificationType(1, "EMAIL_PROMO", "Promotional emails", "Emaile promocyjne")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { clientRepository.save(any()) } answers { firstArg() }
            every { notificationTypeRepository.findAll() } returns listOf(type)
            every { userNotificationSettingRepository.findByClientId(1L) } returns emptyList() andThen listOf(
                setting(c, type, enabled = true, channel = "EMAIL"),
            )
            every { userNotificationSettingRepository.save(any()) } answers { firstArg() }

            val request = NotificationSettingsWrapperDto(
                languageCode = "pl",
                notifications = listOf(NotificationSettingDto(notificationTypeId = 1, channel = "EMAIL", enabled = true, notificationTypeCode = "EMAIL_PROMO", notificationTypeName = ""))
            )
            val result = service.updateNotificationSettings(
                clientNumber,
                request,
                Locale.ENGLISH,
            )

            result.notifications.size shouldBe 3
            result.notifications.first { it.channel == "EMAIL" }.enabled shouldBe true
            verify(exactly = 1) { userNotificationSettingRepository.save(any()) }
            verify(exactly = 1) { clientRepository.save(c) }
        }

        it("updates existing settings") {
            val c = client()
            val type = notificationType(1, "EMAIL_PROMO", "Promotional emails", "Emaile promocyjne")
            val existingSetting = setting(c, type, enabled = false, channel = "SMS")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { clientRepository.save(any()) } answers { firstArg() }
            every { notificationTypeRepository.findAll() } returns listOf(type)
            every { userNotificationSettingRepository.findByClientId(1L) } returns listOf(existingSetting) andThen listOf(
                setting(c, type, enabled = true, channel = "SMS"),
            )
            every { userNotificationSettingRepository.save(any()) } answers { firstArg() }

            val request = NotificationSettingsWrapperDto(
                languageCode = "pl",
                notifications = listOf(NotificationSettingDto(notificationTypeId = 1, channel = "SMS", enabled = true, notificationTypeCode = "EMAIL_PROMO", notificationTypeName = ""))
            )
            val result = service.updateNotificationSettings(
                clientNumber,
                request,
                Locale.ENGLISH,
            )

            result.notifications.first { it.channel == "SMS" }.enabled shouldBe true
            verify(exactly = 1) { userNotificationSettingRepository.save(existingSetting) }
            verify(exactly = 1) { clientRepository.save(c) }
        }

        it("throws NoSuchElementException for invalid notification type ID") {
            val c = client()
            val type = notificationType(1, "EMAIL_PROMO", "Promotional emails", "Emaile promocyjne")
            every { clientRepository.findByClientNumber(clientNumber) } returns c
            every { clientRepository.save(any()) } answers { firstArg() }
            every { notificationTypeRepository.findAll() } returns listOf(type)

            val request = NotificationSettingsWrapperDto(
                languageCode = "pl",
                notifications = listOf(NotificationSettingDto(notificationTypeId = 999, channel = "SMS", enabled = true, notificationTypeCode = "EMAIL_PROMO", notificationTypeName = ""))
            )
            shouldThrow<NoSuchElementException> {
                service.updateNotificationSettings(
                    clientNumber,
                    request,
                    Locale.ENGLISH,
                )
            }
        }

        it("throws NoSuchElementException when client not found") {
            every { clientRepository.findByClientNumber(clientNumber) } returns null

            val request = NotificationSettingsWrapperDto(
                languageCode = "pl",
                notifications = listOf(NotificationSettingDto(notificationTypeId = 1, channel = "SMS", enabled = true, notificationTypeCode = "EMAIL_PROMO", notificationTypeName = ""))
            )
            shouldThrow<NoSuchElementException> {
                service.updateNotificationSettings(
                    clientNumber,
                    request,
                    Locale.ENGLISH,
                )
            }
        }
    }
})
