package repository.ai

import entity.AiProcessingResultEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AiProcessingResultRepository : CrudRepository<AiProcessingResultEntity, UUID>
