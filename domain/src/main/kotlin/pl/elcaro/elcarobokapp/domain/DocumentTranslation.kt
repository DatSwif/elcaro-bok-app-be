package pl.elcaro.elcarobokapp.domain

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
data class DocumentTranslationId(
    @Column(name = "language_code")
    var languageCode: String = "",

    @Column(name = "document_id")
    var documentId: String = "",
) : Serializable

@Entity
@Table(name = "document_translations")
class DocumentTranslation(
    @EmbeddedId
    var id: DocumentTranslationId = DocumentTranslationId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(name = "document_id")
    var document: Document? = null,

    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var text: String = "",
)
