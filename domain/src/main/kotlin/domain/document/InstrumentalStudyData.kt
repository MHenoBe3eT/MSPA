package domain.document

import java.util.UUID

data class InstrumentalStudyDataId(val value: UUID) {
    companion object {
        fun generateId() = InstrumentalStudyDataId(UUID.randomUUID())
    }
}

data class InstrumentalStudyData(
    val id: InstrumentalStudyDataId,
    val medicalDocumentId: MedicalDocumentId,
    val description: String,
    val findings: Map<String, String>?,
)
