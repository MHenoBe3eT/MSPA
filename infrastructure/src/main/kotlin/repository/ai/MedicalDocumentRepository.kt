package repository.ai

import entity.MedicalDocumentEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MedicalDocumentRepository : CrudRepository<MedicalDocumentEntity, UUID>
