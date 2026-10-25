package pl.elcaro.elcarobokapp.domain

import jakarta.persistence.*

@Entity
@Table(name = "documents")
class Document(
    @Id
    var id: String = "",

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var translations: MutableList<DocumentTranslation> = mutableListOf(),
)
