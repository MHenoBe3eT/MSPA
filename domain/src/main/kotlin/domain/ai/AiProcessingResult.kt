package domain.ai

import domain.file.UploadedFileId
import java.time.Instant
import java.util.UUID

data class AiProcessingResultId(val value: UUID) {
    companion object {
        fun generateId() = AiProcessingResultId(UUID.randomUUID())
    }
}

data class AiProcessingResult(
    val id: AiProcessingResultId,
    val uploadedFileId: UploadedFileId,
    val rawExtractedText: String,
    val modelVersion: String,
    val processedAt: Instant,
)
