package pl.elcaro.elcarobokapp.domain.services

import jakarta.persistence.*

@Entity
@Table(name = "tv_channels")
class TVChannel(
    @Column(nullable = false)
    var pictogram: ByteArray
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "tvChannel", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableSet<TVChannelTranslation> = mutableSetOf()
}
