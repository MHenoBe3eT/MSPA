package domain.file

import domain.user.UserId
import java.time.Instant
import java.util.UUID

data class UploadedFileId(val value: UUID) {
    companion object {
        fun generateId() = UploadedFileId(UUID.randomUUID())
    }
}

enum class UploadStatus {
    UPLOADED,
    PROCESSING,
    PROCESSED,
    ERROR,
}

data class UploadedFile(
    val id: UploadedFileId,
    val userId: UserId,
    val originalFileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksum: String,
    val storageKey: String,
    val status: UploadStatus,
    val uploadedAt: Instant,
)
