package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.repository.CrudRepository
import pl.elcaro.elcarobokapp.domain.registration.UserNotificationSetting

interface UserNotificationSettingRepository : CrudRepository<UserNotificationSetting, Long> {
    fun findByClientId(clientId: Long): List<UserNotificationSetting>
}
