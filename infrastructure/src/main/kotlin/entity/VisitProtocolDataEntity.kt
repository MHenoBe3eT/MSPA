package entity

import domain.document.MedicalDocumentId
import domain.document.VisitProtocolData
import domain.document.VisitProtocolDataId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "visit_protocol_data")
class VisitProtocolDataEntity(
    @Id
    var id: UUID,

    @Column(name = "medical_document_id", nullable = false)
    var medicalDocumentId: UUID,

    @Column(name = "narrative_text", nullable = false, columnDefinition = "TEXT")
    var narrativeText: String,

    @Column(name = "complaints", columnDefinition = "TEXT")
    var complaints: String?,

    @Column(name = "anamnesis", columnDefinition = "TEXT")
    var anamnesis: String?,

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    var diagnosis: String?,

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    var treatmentPlan: String?,
) {
    companion object {
        fun toBusiness(e: VisitProtocolDataEntity) = VisitProtocolData(
            id = VisitProtocolDataId(e.id),
            medicalDocumentId = MedicalDocumentId(e.medicalDocumentId),
            narrativeText = e.narrativeText,
            complaints = e.complaints,
            anamnesis = e.anamnesis,
            diagnosis = e.diagnosis,
            treatmentPlan = e.treatmentPlan,
        )

        fun fromBusiness(d: VisitProtocolData) = VisitProtocolDataEntity(
            id = d.id.value,
            medicalDocumentId = d.medicalDocumentId.value,
            narrativeText = d.narrativeText,
            complaints = d.complaints,
            anamnesis = d.anamnesis,
            diagnosis = d.diagnosis,
            treatmentPlan = d.treatmentPlan,
        )
    }
}
