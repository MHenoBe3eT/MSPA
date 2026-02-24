package entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import domain.document.LabAnalysisData
import domain.document.LabAnalysisDataId
import domain.document.LabIndicator
import domain.document.MedicalDocumentId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "lab_analysis_data")
class LabAnalysisDataEntity(
    @Id
    var id: UUID,

    @Column(name = "medical_document_id", nullable = false)
    var medicalDocumentId: UUID,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "indicators", nullable = false, columnDefinition = "jsonb")
    var indicators: String,
) {
    companion object {
        private val mapper = jacksonObjectMapper()
        private val listType = object : TypeReference<List<LabIndicator>>() {}

        fun toBusiness(e: LabAnalysisDataEntity) = LabAnalysisData(
            id = LabAnalysisDataId(e.id),
            medicalDocumentId = MedicalDocumentId(e.medicalDocumentId),
            indicators = mapper.readValue(e.indicators, listType),
        )

        fun fromBusiness(d: LabAnalysisData) = LabAnalysisDataEntity(
            id = d.id.value,
            medicalDocumentId = d.medicalDocumentId.value,
            indicators = mapper.writeValueAsString(d.indicators),
        )
    }
}
