package pl.elcaro.elcarobokapp.service

data class DocumentGroupResponse(
    val label: String,
    val type: String,
    val documents: List<DocumentMetadataResponse>
)

data class DocumentMetadataResponse(
    val id: String,
    val title: String,
    val type: String
)

data class DocumentContentResponse(
    val id: String,
    val title: String?,
    val text: String,
    val type: String?
)
