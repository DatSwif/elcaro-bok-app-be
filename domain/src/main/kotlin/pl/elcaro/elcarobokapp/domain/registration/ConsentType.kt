package pl.elcaro.elcarobokapp.domain.registration

import jakarta.persistence.*

@Entity
@Table(name = "consent_types")
class ConsentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null

    @OneToMany(mappedBy = "consentType", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableList<ConsentTypeTranslation> = mutableListOf()
}
