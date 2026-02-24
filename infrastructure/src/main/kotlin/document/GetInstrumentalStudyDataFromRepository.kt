package document

import domain.document.InstrumentalStudyData
import domain.document.MedicalDocumentId
import entity.InstrumentalStudyDataEntity
import org.springframework.stereotype.Component
import repository.ai.InstrumentalStudyDataRepository

@Component
class GetInstrumentalStudyDataFromRepository(
    private val repository: InstrumentalStudyDataRepository,
) : GetInstrumentalStudyData {

    override fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): InstrumentalStudyData? =
        repository.findByMedicalDocumentId(medicalDocumentId.value)
            ?.let(InstrumentalStudyDataEntity::toBusiness)
}
