package entity

import domain.ai.AiProcessingResult
import domain.ai.AiProcessingResultId
import domain.file.UploadedFileId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ai_processing_results")
class AiProcessingResultEntity(
    @Id
    var id: UUID,

    @Column(name = "uploaded_file_id", nullable = false)
    var uploadedFileId: UUID,

    @Column(name = "raw_extracted_text", nullable = false, columnDefinition = "TEXT")
    var rawExtractedText: String,

    @Column(name = "model_version", nullable = false)
    var modelVersion: String,

    @Column(name = "processed_at", nullable = false)
    var processedAt: Instant,
) {
    companion object {
        fun toBusiness(e: AiProcessingResultEntity) = AiProcessingResult(
            id = AiProcessingResultId(e.id),
            uploadedFileId = UploadedFileId(e.uploadedFileId),
            rawExtractedText = e.rawExtractedText,
            modelVersion = e.modelVersion,
            processedAt = e.processedAt,
        )

        fun fromBusiness(r: AiProcessingResult) = AiProcessingResultEntity(
            id = r.id.value,
            uploadedFileId = r.uploadedFileId.value,
            rawExtractedText = r.rawExtractedText,
            modelVersion = r.modelVersion,
            processedAt = r.processedAt,
        )
    }
}
