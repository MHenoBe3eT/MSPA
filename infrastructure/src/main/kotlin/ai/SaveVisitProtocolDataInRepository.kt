package ai

import domain.document.VisitProtocolData
import entity.VisitProtocolDataEntity
import org.springframework.stereotype.Component
import repository.ai.VisitProtocolDataRepository

@Component
class SaveVisitProtocolDataInRepository(
    private val repository: VisitProtocolDataRepository,
) : SaveVisitProtocolData {
    override fun save(data: VisitProtocolData): VisitProtocolData =
        VisitProtocolDataEntity.toBusiness(repository.save(VisitProtocolDataEntity.fromBusiness(data)))
}
