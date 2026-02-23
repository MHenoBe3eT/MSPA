package repository.ai

import entity.VisitProtocolDataEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface VisitProtocolDataRepository : CrudRepository<VisitProtocolDataEntity, UUID>
