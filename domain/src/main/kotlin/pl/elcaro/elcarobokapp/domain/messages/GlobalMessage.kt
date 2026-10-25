package pl.elcaro.elcarobokapp.domain.messages

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "global_messages")
class GlobalMessage(
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "globalMessage", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableSet<GlobalMessageTranslation> = mutableSetOf()
}
