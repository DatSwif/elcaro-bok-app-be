package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "clients")
class Client(
    @Column(name = "client_number", nullable = false, unique = true, length = 30)
    var clientNumber: String? = null,

    @Column(name = "client_name", length = 50)
    var firstName: String? = null,

    @Column(name = "last_name", length = 50)
    var lastName: String? = null,

    @Column(name = "organization_name", length = 100)
    var organizationName: String? = null,

    @Column(name = "language_code", nullable = false, length = 10)
    var languageCode: String,

    @Column(name = "failed_login_count", nullable = false)
    var failedLoginCount: Int = 0,

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ClientStatus = ClientStatus.NEW,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    var phones: MutableSet<ClientPhone> = linkedSetOf()

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    var emails: MutableSet<ClientEmail> = linkedSetOf()
}