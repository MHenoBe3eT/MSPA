package repository.ai

import entity.InstrumentalStudyDataEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface InstrumentalStudyDataRepository : CrudRepository<InstrumentalStudyDataEntity, UUID> {
    fun findByMedicalDocumentId(medicalDocumentId: UUID): InstrumentalStudyDataEntity?
}
