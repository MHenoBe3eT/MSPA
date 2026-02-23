package repository.ai

import entity.AiInterpretationEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AiInterpretationRepository : CrudRepository<AiInterpretationEntity, UUID> {
    fun findByMedicalDocumentId(medicalDocumentId: UUID): AiInterpretationEntity?
}
