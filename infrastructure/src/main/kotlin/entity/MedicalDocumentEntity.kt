package entity

import domain.DocumentType
import domain.document.MedicalDocument
import domain.document.MedicalDocumentId
import domain.document.MedicalDocumentStatus
import domain.file.UploadedFileId
import domain.user.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "medical_documents")
class MedicalDocumentEntity(
    @Id
    var id: UUID,

    @Column(name = "uploaded_file_id", nullable = false)
    var uploadedFileId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var documentType: DocumentType,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: MedicalDocumentStatus,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "document_date", nullable = false)
    var documentDate: LocalDate,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    companion object {
        fun toBusiness(e: MedicalDocumentEntity) = MedicalDocument(
            id = MedicalDocumentId(e.id),
            uploadedFileId = UploadedFileId(e.uploadedFileId),
            userId = UserId(e.userId),
            documentType = e.documentType,
            status = e.status,
            title = e.title,
            documentDate = e.documentDate,
            createdAt = e.createdAt,
        )

        fun fromBusiness(d: MedicalDocument) = MedicalDocumentEntity(
            id = d.id.value,
            uploadedFileId = d.uploadedFileId.value,
            userId = d.userId.value,
            documentType = d.documentType,
            status = d.status,
            title = d.title,
            documentDate = d.documentDate,
            createdAt = d.createdAt,
        )
    }
}
