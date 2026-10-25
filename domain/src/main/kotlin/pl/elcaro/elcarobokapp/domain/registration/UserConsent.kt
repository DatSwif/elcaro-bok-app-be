package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_consents")
class UserConsent(
    @EmbeddedId
    var id: UserConsentId = UserConsentId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("clientId")
    @JoinColumn(name = "client_id")
    var client: Client,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("consentTypeId")
    @JoinColumn(name = "consent_type_id")
    var consentType: ConsentType,

    @Column(name = "is_granted", nullable = false)
    var isGranted: Boolean = false,

    @Column(name = "granted_at", nullable = false)
    var grantedAt: Instant = Instant.now()
)