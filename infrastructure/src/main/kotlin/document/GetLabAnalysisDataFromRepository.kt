package document

import domain.document.LabAnalysisData
import domain.document.MedicalDocumentId
import entity.LabAnalysisDataEntity
import org.springframework.stereotype.Component
import repository.ai.LabAnalysisDataRepository

@Component
class GetLabAnalysisDataFromRepository(
    private val repository: LabAnalysisDataRepository,
) : GetLabAnalysisData {

    override fun byMedicalDocumentId(medicalDocumentId: MedicalDocumentId): LabAnalysisData? =
        repository.findByMedicalDocumentId(medicalDocumentId.value)
            ?.let(LabAnalysisDataEntity::toBusiness)
}
