package entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import domain.document.InstrumentalStudyData
import domain.document.InstrumentalStudyDataId
import domain.document.MedicalDocumentId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "instrumental_study_data")
class InstrumentalStudyDataEntity(
    @Id
    var id: UUID,

    @Column(name = "medical_document_id", nullable = false)
    var medicalDocumentId: UUID,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "findings", columnDefinition = "jsonb")
    var findings: String?,
) {
    companion object {
        private val mapper = jacksonObjectMapper()
        private val mapType = object : TypeReference<Map<String, String>>() {}

        fun toBusiness(e: InstrumentalStudyDataEntity) = InstrumentalStudyData(
            id = InstrumentalStudyDataId(e.id),
            medicalDocumentId = MedicalDocumentId(e.medicalDocumentId),
            description = e.description,
            findings = e.findings?.let { mapper.readValue(it, mapType) },
        )

        fun fromBusiness(d: InstrumentalStudyData) = InstrumentalStudyDataEntity(
            id = d.id.value,
            medicalDocumentId = d.medicalDocumentId.value,
            description = d.description,
            findings = d.findings?.let { mapper.writeValueAsString(it) },
        )
    }
}
