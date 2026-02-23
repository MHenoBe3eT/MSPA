package entity

import domain.file.UploadedFile
import domain.file.UploadedFileId
import domain.file.UploadStatus
import domain.user.UserId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "uploaded_files")
class UploadedFileEntity(
    @Id
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "original_file_name", nullable = false)
    var originalFileName: String,

    @Column(name = "content_type", nullable = false)
    var contentType: String,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,

    @Column(name = "checksum", nullable = false)
    var checksum: String,

    @Column(name = "storage_key", nullable = false)
    var storageKey: String,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UploadStatus,

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant,
) {
    companion object {
        fun toBusiness(e: UploadedFileEntity): UploadedFile = UploadedFile(
            id = UploadedFileId(e.id),
            userId = UserId(e.userId),
            originalFileName = e.originalFileName,
            contentType = e.contentType,
            sizeBytes = e.sizeBytes,
            checksum = e.checksum,
            storageKey = e.storageKey,
            status = e.status,
            uploadedAt = e.uploadedAt,
        )

        fun fromBusiness(f: UploadedFile): UploadedFileEntity = UploadedFileEntity(
            id = f.id.value,
            userId = f.userId.value,
            originalFileName = f.originalFileName,
            contentType = f.contentType,
            sizeBytes = f.sizeBytes,
            checksum = f.checksum,
            storageKey = f.storageKey,
            status = f.status,
            uploadedAt = f.uploadedAt,
        )
    }
}
