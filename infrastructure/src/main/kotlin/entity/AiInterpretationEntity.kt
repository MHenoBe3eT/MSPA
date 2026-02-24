package entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import domain.document.AiInterpretation
import domain.document.AiInterpretationId
import domain.document.MedicalDocumentId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "ai_interpretations")
class AiInterpretationEntity(
    @Id
    var id: UUID,

    @Column(name = "medical_document_id", nullable = false)
    var medicalDocumentId: UUID,

    @Column(name = "interpretation_text", nullable = false, columnDefinition = "TEXT")
    var interpretationText: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_markers", nullable = false, columnDefinition = "jsonb")
    var riskMarkers: String,

    @Column(name = "disclaimer", nullable = false, columnDefinition = "TEXT")
    var disclaimer: String,

    @Column(name = "model_version", nullable = false)
    var modelVersion: String,
) {
    companion object {
        private val mapper = jacksonObjectMapper()
        private val listType = object : TypeReference<List<String>>() {}

        fun toBusiness(e: AiInterpretationEntity) = AiInterpretation(
            id = AiInterpretationId(e.id),
            medicalDocumentId = MedicalDocumentId(e.medicalDocumentId),
            interpretationText = e.interpretationText,
            riskMarkers = mapper.readValue(e.riskMarkers, listType),
            disclaimer = e.disclaimer,
            modelVersion = e.modelVersion,
        )

        fun fromBusiness(i: AiInterpretation) = AiInterpretationEntity(
            id = i.id.value,
            medicalDocumentId = i.medicalDocumentId.value,
            interpretationText = i.interpretationText,
            riskMarkers = mapper.writeValueAsString(i.riskMarkers),
            disclaimer = i.disclaimer,
            modelVersion = i.modelVersion,
        )
    }
}
