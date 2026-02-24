package domain.document

import java.util.UUID

data class AiInterpretationId(val value: UUID) {
    companion object {
        fun generateId() = AiInterpretationId(UUID.randomUUID())
    }
}

data class AiInterpretation(
    val id: AiInterpretationId,
    val medicalDocumentId: MedicalDocumentId,
    val interpretationText: String,
    val riskMarkers: List<String>,
    val disclaimer: String,
    val modelVersion: String,
)
