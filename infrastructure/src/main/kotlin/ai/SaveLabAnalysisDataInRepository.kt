package ai

import domain.document.LabAnalysisData
import entity.LabAnalysisDataEntity
import org.springframework.stereotype.Component
import repository.ai.LabAnalysisDataRepository

@Component
class SaveLabAnalysisDataInRepository(
    private val repository: LabAnalysisDataRepository,
) : SaveLabAnalysisData {
    override fun save(data: LabAnalysisData): LabAnalysisData =
        LabAnalysisDataEntity.toBusiness(repository.save(LabAnalysisDataEntity.fromBusiness(data)))
}
