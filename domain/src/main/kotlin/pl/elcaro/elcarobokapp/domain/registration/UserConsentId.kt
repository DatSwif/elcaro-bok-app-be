package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserConsentId(
    @Column(name = "client_id", nullable = false)
    var clientId: Long? = null,

    @Column(name = "consent_type_id", nullable = false)
    var consentTypeId: Int? = null
) : Serializable