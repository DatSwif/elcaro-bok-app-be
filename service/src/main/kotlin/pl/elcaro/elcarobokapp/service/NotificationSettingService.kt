package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.registration.UserNotificationSetting
import pl.elcaro.elcarobokapp.infrastructure.ClientRepository
import pl.elcaro.elcarobokapp.infrastructure.NotificationTypeRepository
import pl.elcaro.elcarobokapp.infrastructure.UserNotificationSettingRepository
import java.util.*

@Service
@Transactional
class NotificationSettingService(
    private val clientRepository: ClientRepository,
    private val notificationTypeRepository: NotificationTypeRepository,
    private val userNotificationSettingRepository: UserNotificationSettingRepository,
) {
    private val expectedChannels = listOf("SYSTEM", "EMAIL", "SMS")

    @Transactional(readOnly = true)
    fun getNotificationSettings(clientNumber: String, locale: Locale): NotificationSettingsWrapperDto {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client with number $clientNumber not found")

        val allTypes = notificationTypeRepository.findAll()
        val allClientSettings = userNotificationSettingRepository.findByClientId(client.id!!)
        
        val settingsDtoList = mutableListOf<NotificationSettingDto>()
        
        for (type in allTypes) {
            val typeTranslation = type.translations.firstOrNull { it.id.languageCode == locale.language }
                ?: type.translations.firstOrNull { it.id.languageCode == "pl" }
                ?: type.translations.firstOrNull()
                
            val typeName = typeTranslation?.name ?: type.code
            
            for (channel in expectedChannels) {
                val existingSetting = allClientSettings.find { 
                    it.notificationType.id == type.id && it.channel == channel 
                }
                
                settingsDtoList.add(
                    NotificationSettingDto(
                        notificationTypeId = type.id!!,
                        notificationTypeCode = type.code,
                        notificationTypeName = typeName,
                        channel = channel,
                        enabled = existingSetting?.isEnabled ?: false
                    )
                )
            }
        }

        return NotificationSettingsWrapperDto(
            languageCode = client.languageCode,
            notifications = settingsDtoList
        )
    }

    fun updateNotificationSettings(
        clientNumber: String,
        wrapper: NotificationSettingsWrapperDto,
        locale: Locale,
    ): NotificationSettingsWrapperDto {
        val client = clientRepository.findByClientNumber(clientNumber)
            ?: throw NoSuchElementException("Client with number $clientNumber not found")

        // Update language
        client.languageCode = wrapper.languageCode
        clientRepository.save(client)

        val allTypes = notificationTypeRepository.findAll()
        val allTypeIds = allTypes.map { it.id }.toSet()
        val typesById = allTypes.associateBy { it.id }

        val requests = wrapper.notifications
        val invalidIds = requests.map { it.notificationTypeId }.filter { it !in allTypeIds }
        if (invalidIds.isNotEmpty()) {
            throw NoSuchElementException("Notification types not found: $invalidIds")
        }

        val existingSettingsList = userNotificationSettingRepository.findByClientId(client.id!!)

        for (request in requests) {
            val existing = existingSettingsList.find { 
                it.notificationType.id == request.notificationTypeId && it.channel == request.channel 
            }
            
            if (existing != null) {
                existing.isEnabled = request.enabled
                userNotificationSettingRepository.save(existing)
            } else {
                userNotificationSettingRepository.save(
                    UserNotificationSetting(
                        client = client,
                        notificationType = typesById[request.notificationTypeId]!!,
                        channel = request.channel,
                        isEnabled = request.enabled,
                    )
                )
            }
        }

        return getNotificationSettings(clientNumber, locale)
    }
}
