package domain.document

import domain.DocumentType
import java.time.LocalDate
import java.util.*

data class Document(
    val id: DocumentId,
    val documentType: DocumentType,
    val title: String,
    val creationDate: LocalDate,
    val visitDate: LocalDate,
    val filePath: String,
    val extractedText: String,
) {
    companion object {
        fun createNew(
            id: String,
            documentType: DocumentType,
            title: String,
            creationDate: LocalDate,
            visitDate: LocalDate,
            filePath: String,
            extractedText: String
        ): Document = Document(
            id = DocumentId(UUID.fromString(id)),
            documentType = documentType,
            title = title,
            creationDate = creationDate,
            visitDate = visitDate,
            filePath = filePath,
            extractedText = extractedText
        )
    }
}

data class DocumentId(val value: UUID) {
    companion object {
        fun generateId(): DocumentId = DocumentId(UUID.randomUUID())
    }
}
