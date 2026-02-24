package repository.ai

import entity.LabAnalysisDataEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface LabAnalysisDataRepository : CrudRepository<LabAnalysisDataEntity, UUID> {
    fun findByMedicalDocumentId(medicalDocumentId: UUID): LabAnalysisDataEntity?
}
