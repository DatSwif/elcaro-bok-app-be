package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*

@Entity
@Table(name = "hardware")
class Hardware(
    @Column(name = "image_path", columnDefinition = "TEXT")
    var imagePath: String? = null,

    @Column(nullable = false)
    var isReturnable: Boolean = false,

    @Column(nullable = false)
    var isReportable: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "hardware", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableSet<HardwareTranslation> = mutableSetOf()
}
