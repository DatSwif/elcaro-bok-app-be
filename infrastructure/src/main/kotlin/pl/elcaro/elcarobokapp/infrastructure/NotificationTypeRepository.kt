package pl.elcaro.elcarobokapp.infrastructure

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import pl.elcaro.elcarobokapp.domain.registration.NotificationType

interface NotificationTypeRepository : JpaRepository<NotificationType, Int> {

    @EntityGraph(attributePaths = ["translations"])
    override fun findAll(): List<NotificationType>
}
