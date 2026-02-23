package domain.document

import java.util.UUID

data class LabAnalysisDataId(val value: UUID) {
    companion object {
        fun generateId() = LabAnalysisDataId(UUID.randomUUID())
    }
}

data class LabIndicator(
    val name: String,
    val code: String?,
    val value: String,
    val unit: String?,
    val referenceRange: String?,
)

data class LabAnalysisData(
    val id: LabAnalysisDataId,
    val medicalDocumentId: MedicalDocumentId,
    val indicators: List<LabIndicator>,
)
