package ai

import domain.document.InstrumentalStudyData
import entity.InstrumentalStudyDataEntity
import org.springframework.stereotype.Component
import repository.ai.InstrumentalStudyDataRepository

@Component
class SaveInstrumentalStudyDataInRepository(
    private val repository: InstrumentalStudyDataRepository,
) : SaveInstrumentalStudyData {
    override fun save(data: InstrumentalStudyData): InstrumentalStudyData =
        InstrumentalStudyDataEntity.toBusiness(repository.save(InstrumentalStudyDataEntity.fromBusiness(data)))
}
