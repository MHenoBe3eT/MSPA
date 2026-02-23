package domain.document

import java.util.UUID

data class VisitProtocolDataId(val value: UUID) {
    companion object {
        fun generateId() = VisitProtocolDataId(UUID.randomUUID())
    }
}

data class VisitProtocolData(
    val id: VisitProtocolDataId,
    val medicalDocumentId: MedicalDocumentId,
    val narrativeText: String,
    val complaints: String?,
    val anamnesis: String?,
    val diagnosis: String?,
    val treatmentPlan: String?,
)
