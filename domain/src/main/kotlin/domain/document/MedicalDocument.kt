package domain.document

import domain.DocumentType
import domain.file.UploadedFileId
import domain.user.UserId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MedicalDocumentId(val value: UUID) {
    companion object {
        fun generateId() = MedicalDocumentId(UUID.randomUUID())
    }
}

enum class MedicalDocumentStatus {
    PROCESSING,
    PROCESSED,
    ERROR,
}

data class MedicalDocument(
    val id: MedicalDocumentId,
    val uploadedFileId: UploadedFileId,
    val userId: UserId,
    val documentType: DocumentType,
    val status: MedicalDocumentStatus,
    val title: String,
    val documentDate: LocalDate,
    val createdAt: Instant,
)
